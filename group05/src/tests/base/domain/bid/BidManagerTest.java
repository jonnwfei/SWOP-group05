package tests.base.domain.bid;

import base.domain.bid.Bid;
import base.domain.bid.BidManager;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("BidManager")
class BidManagerTest {

    @Mock private Player p1;
    @Mock private Player p2;
    @Mock private Player p3;
    @Mock private Player p4;

    private final PlayerId id1 = new PlayerId();
    private final PlayerId id2 = new PlayerId();
    private final PlayerId id3 = new PlayerId();
    private final PlayerId id4 = new PlayerId();

    private AutoCloseable mocks;
    private List<Player> players;
    private BidManager manager;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        lenient().when(p1.getId()).thenReturn(id1);
        lenient().when(p2.getId()).thenReturn(id2);
        lenient().when(p3.getId()).thenReturn(id3);
        lenient().when(p4.getId()).thenReturn(id4);
        players = List.of(p1, p2, p3, p4);
        manager = new BidManager(players);
    }

    @AfterEach
    void tearDown() throws Exception { mocks.close(); }

    // =========================================================================
    // Constructor
    // =========================================================================

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("Rejects null player list")
        void rejectsNull() {
            assertThrows(IllegalArgumentException.class, () -> new BidManager(null));
        }

        @Test
        @DisplayName("Rejects fewer than 4 players")
        void rejectsTooFew() {
            assertThrows(IllegalArgumentException.class,
                    () -> new BidManager(List.of(p1, p2, p3)));
        }

        @Test
        @DisplayName("Rejects more than 4 players")
        void rejectsTooMany() {
            Player p5 = mock(Player.class);
            assertThrows(IllegalArgumentException.class,
                    () -> new BidManager(List.of(p1, p2, p3, p4, p5)));
        }

        @Test
        @DisplayName("Accepts exactly 4 players")
        void acceptsFour() {
            assertDoesNotThrow(() -> new BidManager(players));
        }
    }

    // =========================================================================
    // placeBid / tracking
    // =========================================================================

    @Nested
    @DisplayName("placeBid")
    class PlaceBidTests {

        @Test
        @DisplayName("PASS does not become the highest bid")
        void passNeverHighest() {
            manager.placeBid(id1, BidType.PASS, null);
            assertNull(manager.getHighestBid());
            assertNull(manager.getHighestBidder());
        }

        @Test
        @DisplayName("First non-PASS bid becomes the highest")
        void firstNonPassBecomesHighest() {
            manager.placeBid(id1, BidType.PROPOSAL, null);
            assertNotNull(manager.getHighestBid());
            assertEquals(BidType.PROPOSAL, manager.getHighestBid().getType());
            assertEquals(id1, manager.getHighestBidder());
        }

        @Test
        @DisplayName("Higher bid replaces the highest")
        void higherBidReplaces() {
            manager.placeBid(id1, BidType.PROPOSAL, null);
            manager.placeBid(id2, BidType.ACCEPTANCE, null);
            assertEquals(BidType.ACCEPTANCE, manager.getHighestBid().getType());
            assertEquals(id2, manager.getHighestBidder());
        }

        @Test
        @DisplayName("Lower bid does not replace the highest")
        void lowerBidIgnored() {
            manager.placeBid(id1, BidType.ACCEPTANCE, null);
            manager.placeBid(id2, BidType.PROPOSAL, null);
            assertEquals(BidType.ACCEPTANCE, manager.getHighestBid().getType());
            assertEquals(id1, manager.getHighestBidder());
        }

        @Test
        @DisplayName("hasBid returns true after placement")
        void hasBidAfterPlacement() {
            assertFalse(manager.hasBid(id1));
            manager.placeBid(id1, BidType.PASS, null);
            assertTrue(manager.hasBid(id1));
        }

        @Test
        @DisplayName("Throws on null playerId")
        void throwsOnNullPlayerId() {
            assertThrows(IllegalArgumentException.class,
                    () -> manager.placeBid(null, BidType.PASS, null));
        }

        @Test
        @DisplayName("Throws on null bidType")
        void throwsOnNullBidType() {
            assertThrows(IllegalArgumentException.class,
                    () -> manager.placeBid(id1, null, null));
        }

        @Test
        @DisplayName("isBiddingComplete only after all 4 players have bid")
        void biddingComplete() {
            assertFalse(manager.isBiddingComplete());
            manager.placeBid(id1, BidType.PASS, null);
            manager.placeBid(id2, BidType.PASS, null);
            manager.placeBid(id3, BidType.PASS, null);
            assertFalse(manager.isBiddingComplete());
            manager.placeBid(id4, BidType.PASS, null);
            assertTrue(manager.isBiddingComplete());
        }
    }

    // =========================================================================
    // invalidateProposal
    // =========================================================================

    @Nested
    @DisplayName("invalidateProposal")
    class InvalidateProposalTests {

        @Test
        @DisplayName("Replaces proposer's bid with PASS and recomputes highest")
        void replacesProposalWithPass() {
            manager.placeBid(id1, BidType.PROPOSAL, null);
            assertEquals(BidType.PROPOSAL, manager.getHighestBid().getType());

            manager.invalidateProposal();

            assertNull(manager.getHighestBid(),
                    "Highest bid should be null after proposal invalidated");
            assertNull(manager.findProposer());
        }

        @Test
        @DisplayName("No-op when there is no proposer")
        void noOpWithoutProposer() {
            manager.placeBid(id1, BidType.PASS, null);
            assertDoesNotThrow(() -> manager.invalidateProposal());
        }

        @Test
        @DisplayName("Restores previous highest bid after proposal removed")
        void restoresPreviousHighest() {
            // p1 places acceptance first, then p2 proposes
            manager.placeBid(id1, BidType.ACCEPTANCE, null);
            manager.placeBid(id2, BidType.PROPOSAL, null);
            // highest is ACCEPTANCE (outranks PROPOSAL); invalidating proposal keeps it
            manager.invalidateProposal();
            assertEquals(BidType.ACCEPTANCE, manager.getHighestBid().getType());
        }
    }

    // =========================================================================
    // isLegalBid
    // =========================================================================

    @Nested
    @DisplayName("isLegalBid")
    class IsLegalBidTests {

        @Test
        @DisplayName("PASS is always legal")
        void passAlwaysLegal() {
            assertTrue(manager.isLegalBid(BidType.PASS));
        }

        @Test
        @DisplayName("ACCEPTANCE is legal when current highest is PROPOSAL")
        void acceptanceLegalAfterProposal() {
            manager.placeBid(id1, BidType.PROPOSAL, null);
            assertTrue(manager.isLegalBid(BidType.ACCEPTANCE));
        }

        @Test
        @DisplayName("ACCEPTANCE is illegal when no proposal exists")
        void acceptanceIllegalWithoutProposal() {
            assertFalse(manager.isLegalBid(BidType.ACCEPTANCE));
        }

        @Test
        @DisplayName("ACCEPTANCE is illegal when highest is not PROPOSAL")
        void acceptanceIllegalWhenHighestIsNotProposal() {
            manager.placeBid(id1, BidType.ACCEPTANCE, null);
            assertFalse(manager.isLegalBid(BidType.ACCEPTANCE));
        }

        @Test
        @DisplayName("SOLO_PROPOSAL is legal only when bidding is complete")
        void soloProposalLegalAfterAllBid() {
            assertFalse(manager.isLegalBid(BidType.SOLO_PROPOSAL));
            manager.placeBid(id1, BidType.PASS, null);
            manager.placeBid(id2, BidType.PASS, null);
            manager.placeBid(id3, BidType.PASS, null);
            manager.placeBid(id4, BidType.PASS, null);
            assertTrue(manager.isLegalBid(BidType.SOLO_PROPOSAL));
        }

        @Test
        @DisplayName("TROEL is never a legal chosen bid")
        void troelNeverLegal() {
            assertFalse(manager.isLegalBid(BidType.TROEL));
        }

        @Test
        @DisplayName("TROELA is never a legal chosen bid")
        void troelaIsNeverLegal() {
            assertFalse(manager.isLegalBid(BidType.TROELA));
        }

        @Test
        @DisplayName("Bid lower than current highest is illegal")
        void lowerBidIllegal() {
            manager.placeBid(id1, BidType.ACCEPTANCE, null);
            assertFalse(manager.isLegalBid(BidType.PROPOSAL));
        }

        @Test
        @DisplayName("Equal non-MISERIE bid is illegal")
        void equalNonMiserieIllegal() {
            manager.placeBid(id1, BidType.ACCEPTANCE, null);
            assertFalse(manager.isLegalBid(BidType.ACCEPTANCE));
        }

        @Test
        @DisplayName("Throws on null BidType")
        void throwsOnNull() {
            assertThrows(IllegalArgumentException.class, () -> manager.isLegalBid(null));
        }
    }

    // =========================================================================
    // findProposer / findAcceptor / findMiserieParticipants
    // =========================================================================

    @Nested
    @DisplayName("Partner queries")
    class PartnerQueryTests {

        @Test
        @DisplayName("findProposer returns null when no proposal placed")
        void noProposer() {
            assertNull(manager.findProposer());
        }

        @Test
        @DisplayName("findProposer returns the proposer's id")
        void findsProposer() {
            manager.placeBid(id2, BidType.PROPOSAL, null);
            assertEquals(id2, manager.findProposer());
        }

        @Test
        @DisplayName("findAcceptor returns the acceptor's id")
        void findsAcceptor() {
            manager.placeBid(id1, BidType.PROPOSAL, null);
            manager.placeBid(id2, BidType.ACCEPTANCE, null);
            assertEquals(id2, manager.findAcceptor());
        }

        @Test
        @DisplayName("findMiserieParticipants returns all miserie bidders")
        void findsMiserieParticipants() {
            manager.placeBid(id1, BidType.MISERIE, null);
            manager.placeBid(id2, BidType.MISERIE, null);
            manager.placeBid(id3, BidType.PASS, null);
            manager.placeBid(id4, BidType.PASS, null);
            List<PlayerId> participants = manager.findMiserieParticipants(BidType.MISERIE);
            assertEquals(2, participants.size());
            assertTrue(participants.containsAll(List.of(id1, id2)));
        }

        @Test
        @DisplayName("findMiserieParticipants throws on non-miserie type")
        void throwsOnNonMiserieType() {
            assertThrows(IllegalArgumentException.class,
                    () -> manager.findMiserieParticipants(BidType.SOLO));
        }

        @Test
        @DisplayName("findMiserieParticipants throws on null type")
        void throwsOnNullType() {
            assertThrows(IllegalArgumentException.class,
                    () -> manager.findMiserieParticipants(null));
        }
    }

    // =========================================================================
    // getBidderOf
    // =========================================================================

    @Nested
    @DisplayName("getBidderOf")
    class GetBidderOfTests {

        @Test
        @DisplayName("Returns the correct bidder for a registered bid")
        void returnsCorrectBidder() {
            Bid placed = manager.placeBid(id3, BidType.SOLO, Suit.HEARTS);
            assertEquals(id3, manager.getBidderOf(placed));
        }

        @Test
        @DisplayName("Throws for an unregistered bid object")
        void throwsForUnknownBid() {
            Bid unregistered = BidType.PASS.instantiate(null);
            assertThrows(IllegalArgumentException.class,
                    () -> manager.getBidderOf(unregistered));
        }
    }

    // =========================================================================
    // detectForcedBid / findMissingAceSuit
    // =========================================================================

    @Nested
    @DisplayName("detectForcedBid and findMissingAceSuit")
    class ForcedBidTests {

        private List<Card> handWithAces(Suit... aceSuits) {
            java.util.ArrayList<Card> hand = new java.util.ArrayList<>();
            for (Suit s : aceSuits) hand.add(new Card(s, Rank.ACE));
            int i = 0;
            while (hand.size() < 13) {
                hand.add(new Card(Suit.CLUBS, Rank.values()[i++ % 13]));
            }
            return hand;
        }

        @Test
        @DisplayName("4 aces → TROELA forced bid detected")
        void fourAcesIsTroela() {
            when(p1.getHand()).thenReturn(handWithAces(
                    Suit.HEARTS, Suit.SPADES, Suit.CLUBS, Suit.DIAMONDS));
            BidType result = manager.detectForcedBid(p1);
            assertNotNull(result);
            assertEquals(BidType.TROELA, result);
        }

        @Test
        @DisplayName("3 aces → TROEL forced bid detected")
        void threeAcesIsTroel() {
            when(p1.getHand()).thenReturn(handWithAces(
                    Suit.HEARTS, Suit.SPADES, Suit.CLUBS));
            BidType result = manager.detectForcedBid(p1);
            assertNotNull(result);
            assertEquals(BidType.TROEL, result);
        }

        @Test
        @DisplayName("2 aces → no forced bid (returns null)")
        void twoAcesNoForce() {
            when(p1.getHand()).thenReturn(handWithAces(Suit.HEARTS, Suit.SPADES));
            assertNull(manager.detectForcedBid(p1));
        }

        @Test
        @DisplayName("findMissingAceSuit returns the suit of the missing ace")
        void findsMissingAceSuit() {
            when(p1.getHand()).thenReturn(handWithAces(
                    Suit.HEARTS, Suit.SPADES, Suit.CLUBS)); // DIAMONDS missing
            assertEquals(Suit.DIAMONDS, manager.findMissingAceSuit(p1));
        }

        @Test
        @DisplayName("findMissingAceSuit throws when player holds all 4 aces")
        void throwsWhenFourAces() {
            when(p1.getHand()).thenReturn(handWithAces(
                    Suit.HEARTS, Suit.SPADES, Suit.CLUBS, Suit.DIAMONDS));
            assertThrows(IllegalStateException.class,
                    () -> manager.findMissingAceSuit(p1));
        }
    }

    // =========================================================================
    // resolveBiddingTeam
    // =========================================================================

    @Nested
    @DisplayName("resolveBiddingTeam")
    class ResolveBiddingTeamTests {

        @Test
        @DisplayName("Throws when no highest bid exists")
        void throwsWithNoBid() {
            assertThrows(IllegalStateException.class, () -> manager.resolveBiddingTeam());
        }

        @Test
        @DisplayName("SOLO bid → team of 1 (the bidder)")
        void soloBidTeamOfOne() {
            manager.placeBid(id1, BidType.SOLO, Suit.HEARTS);
            List<PlayerId> team = manager.resolveBiddingTeam();
            assertEquals(List.of(id1), team);
        }

        @Test
        @DisplayName("PROPOSAL + ACCEPTANCE → team of proposer and acceptor")
        void proposalAcceptanceTeamOfTwo() {
            manager.placeBid(id1, BidType.PROPOSAL, null);
            manager.placeBid(id2, BidType.ACCEPTANCE, null);
            List<PlayerId> team = manager.resolveBiddingTeam();
            assertEquals(2, team.size());
            assertTrue(team.containsAll(List.of(id1, id2)));
        }

        @Test
        @DisplayName("MISERIE → team is all miserie participants")
        void miserieTeamIsAllParticipants() {
            manager.placeBid(id1, BidType.MISERIE, null);
            manager.placeBid(id2, BidType.MISERIE, null);
            manager.placeBid(id3, BidType.PASS, null);
            manager.placeBid(id4, BidType.PASS, null);
            List<PlayerId> team = manager.resolveBiddingTeam();
            assertEquals(2, team.size());
            assertTrue(team.containsAll(List.of(id1, id2)));
        }

        @Test
        @DisplayName("ABONDANCE → solo team (just the bidder)")
        void abondanceTeamOfOne() {
            manager.placeBid(id3, BidType.ABONDANCE_9, Suit.SPADES);
            List<PlayerId> team = manager.resolveBiddingTeam();
            assertEquals(List.of(id3), team);
        }
    }

    // =========================================================================
    // getAllBids
    // =========================================================================

    @Nested
    @DisplayName("getAllBids")
    class GetAllBidsTests {

        @Test
        @DisplayName("Returns bids in placement order")
        void preservesInsertionOrder() {
            manager.placeBid(id1, BidType.PROPOSAL, null);
            manager.placeBid(id2, BidType.ACCEPTANCE, null);
            manager.placeBid(id3, BidType.PASS, null);
            manager.placeBid(id4, BidType.PASS, null);
            List<Bid> bids = manager.getAllBids();
            assertEquals(4, bids.size());
            assertEquals(BidType.PROPOSAL, bids.get(0).getType());
            assertEquals(BidType.ACCEPTANCE, bids.get(1).getType());
        }
    }
}
