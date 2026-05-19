package base.domain.round;

import base.domain.bid.*;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.trick.Trick;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Round Domain Entity Tests")
class RoundTest {

    @Mock private Player p1;
    @Mock private Player p2;
    @Mock private Player p3;
    @Mock private Player p4;
    @Mock private Player externalPlayer;

    private AutoCloseable mocks;
    private List<Player> players;
    private Round round;

    private final PlayerId id1 = new PlayerId();
    private final PlayerId id2 = new PlayerId();
    private final PlayerId id3 = new PlayerId();
    private final PlayerId id4 = new PlayerId();

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        lenient().when(p1.getId()).thenReturn(id1);
        lenient().when(p2.getId()).thenReturn(id2);
        lenient().when(p3.getId()).thenReturn(id3);
        lenient().when(p4.getId()).thenReturn(id4);
        lenient().when(externalPlayer.getId()).thenReturn(new PlayerId());

        lenient().when(p1.getHand()).thenReturn(createDummyHand(13));
        lenient().when(p2.getHand()).thenReturn(createDummyHand(13));
        lenient().when(p3.getHand()).thenReturn(createDummyHand(13));
        lenient().when(p4.getHand()).thenReturn(createDummyHand(13));

        players = List.of(p1, p2, p3, p4);
        round = new Round(players, p1, 2);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) mocks.close();
    }

    @Nested
    @DisplayName("Constructor & Initialization Guards")
    class ConstructorTests {

        @Test
        @DisplayName("Successfully initializes with 4 valid players and a fresh BidManager")
        void shouldInitializeSuccessfully() {
            assertEquals(4, round.getPlayers().size());
            assertEquals(p1, round.getCurrentPlayer());
            assertEquals(2, round.getMultiplier());
            assertFalse(round.isFinished());
            assertEquals(List.of(0, 0, 0, 0), round.getScoreDeltas());
            assertNotNull(round.getBidManager(), "Round must expose its BidManager");
            assertTrue(round.getBidManager().getAllBids().isEmpty());
        }

        @Test
        @DisplayName("Rejects null or incorrectly sized player lists")
        void shouldRejectInvalidPlayerLists() {
            assertThrows(IllegalArgumentException.class, () -> new Round(null, p1, 1));
            assertThrows(IllegalArgumentException.class, () -> new Round(List.of(p1, p2, p3), p1, 1));
            assertThrows(IllegalArgumentException.class, () -> new Round(List.of(p1, p2, p3, p4, externalPlayer), p1, 1));
        }

        @Test
        @DisplayName("Rejects invalid starting players")
        void shouldRejectInvalidStartingPlayers() {
            assertThrows(IllegalArgumentException.class, () -> new Round(players, null, 1));
            assertThrows(IllegalArgumentException.class, () -> new Round(players, externalPlayer, 1));
        }
    }

    @Nested
    @DisplayName("Bidding Phase Transitions")
    class BiddingPhaseTests {

        @Test
        @DisplayName("startPlayPhase enforces non-null parameters and correct bid size")
        void startPlayPhase_Validation() {
            round.getBidManager().placeBid(id1, BidType.SOLO, Suit.HEARTS);
            round.getBidManager().placeBid(id2, BidType.PASS, null);
            round.getBidManager().placeBid(id3, BidType.PASS, null);
            round.getBidManager().placeBid(id4, BidType.PASS, null);
            Bid highestBid = round.getBidManager().getHighestBid().get();
            List<Bid> validBids = List.of(highestBid, new PassBid(), new PassBid(), new PassBid());

            assertThrows(IllegalArgumentException.class, () -> round.startPlayPhase(null, highestBid, Suit.HEARTS, p1));
            assertThrows(IllegalArgumentException.class, () -> round.startPlayPhase(validBids.subList(0, 3), highestBid, Suit.HEARTS, p1));
            assertThrows(IllegalArgumentException.class, () -> round.startPlayPhase(validBids, null, Suit.HEARTS, p1));
            assertThrows(IllegalArgumentException.class, () -> round.startPlayPhase(validBids, highestBid, Suit.HEARTS, null));
        }

        @Test
        @DisplayName("startPlayPhase resolves the bidding team via BidManager and locks in state")
        void startPlayPhase_Success() {
            // Proposal by p1, accepted by p2 — manager-resolved team = {p1, p2}
            round.getBidManager().placeBid(id1, BidType.PROPOSAL, null);
            round.getBidManager().placeBid(id2, BidType.ACCEPTANCE, null);
            round.getBidManager().placeBid(id3, BidType.PASS, null);
            round.getBidManager().placeBid(id4, BidType.PASS, null);
            Bid acceptanceBid = round.getBidManager().getHighestBid().get();

            List<Bid> bids = List.of(new ProposalBid(), acceptanceBid, new PassBid(), new PassBid());
            round.startPlayPhase(bids, acceptanceBid, Suit.SPADES, p3);

            assertEquals(acceptanceBid, round.getHighestBid());
            assertEquals(Suit.SPADES, round.getTrumpSuit());
            assertEquals(p3, round.getCurrentPlayer());
            assertEquals(2, round.getBiddingTeamPlayers().size());
            assertTrue(round.getBiddingTeamPlayers().containsAll(List.of(p1, p2)));
        }

        @Test
        @DisplayName("resolveTeams throws if cards do not sum to 52")
        void resolveTeams_HandValidation() {
            Bid soloBid = round.getBidManager().placeBid(id1, BidType.SOLO, Suit.HEARTS);
            round.getBidManager().placeBid(id2, BidType.PASS, null);
            round.getBidManager().placeBid(id3, BidType.PASS, null);
            round.getBidManager().placeBid(id4, BidType.PASS, null);

            List<Bid> bids = List.of(soloBid, new PassBid(), new PassBid(), new PassBid());
            when(p1.getHand()).thenReturn(createDummyHand(1)); // breaks the 52-card invariant

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> round.startPlayPhase(bids, soloBid, Suit.SPADES, p3));
            assertTrue(ex.getMessage().contains("before the play phase begins"));
        }

        @Test
        @DisplayName("abortWithAllPass validation guards")
        void abortWithAllPass_Validation() {
            Bid passBid = new PassBid();
            Bid soloBid = BidType.SOLO.instantiate(Suit.HEARTS);

            assertThrows(IllegalArgumentException.class, () -> round.abortWithAllPass(null));
            assertThrows(IllegalArgumentException.class, () -> round.abortWithAllPass(List.of(passBid)));
            assertThrows(IllegalArgumentException.class,
                    () -> round.abortWithAllPass(List.of(passBid, passBid, passBid, soloBid)),
                    "All bids must be PASS");
        }

        @Test
        @DisplayName("abortWithAllPass flushes hands and ends round")
        void abortWithAllPass_Success() {
            Bid passBid = new PassBid();
            List<Bid> passBids = List.of(passBid, passBid, passBid, passBid);

            round.abortWithAllPass(passBids);

            assertTrue(round.isFinished());
            verify(p1).flushHand();
            assertEquals(passBid, round.getHighestBid());
        }
    }

    @Nested
    @DisplayName("Play Phase Mechanics")
    class PlayPhaseTests {

        @Test
        @DisplayName("advanceToNextPlayer correctly loops (modulo 4)")
        void advanceToNextPlayer_Looping() {
            assertEquals(p1, round.getCurrentPlayer());
            round.advanceToNextPlayer(); assertEquals(p2, round.getCurrentPlayer());
            round.advanceToNextPlayer(); assertEquals(p3, round.getCurrentPlayer());
            round.advanceToNextPlayer(); assertEquals(p4, round.getCurrentPlayer());
            round.advanceToNextPlayer(); assertEquals(p1, round.getCurrentPlayer());
        }

        @Test
        @DisplayName("finalizeTrick validation guards")
        void finalizeTrick_Validation() {
            Trick incompleteTrick = mock(Trick.class);
            when(incompleteTrick.getTurns()).thenReturn(List.of(mock(), mock()));

            assertThrows(IllegalArgumentException.class, () -> round.finalizeTrick(null));
            assertThrows(IllegalArgumentException.class, () -> round.finalizeTrick(incompleteTrick));

            round.abortWithAllPass(createFourPassBids());
            Trick validTrick = mockCompletedTrick(id1);
            assertThrows(IllegalStateException.class, () -> round.finalizeTrick(validTrick),
                    "Cannot add trick to finished round");
        }

        @Test
        @DisplayName("finalizeTrick assigns next turn to winner and checks auto-finish")
        void finalizeTrick_SuccessAndAutoFinish() {
            // SOLO bid: p1 bidder (team = {p1}), p3 wins all 13 tricks → bidder fails
            Bid soloBid = round.getBidManager().placeBid(id1, BidType.SOLO, Suit.HEARTS);
            round.getBidManager().placeBid(id2, BidType.PASS, null);
            round.getBidManager().placeBid(id3, BidType.PASS, null);
            round.getBidManager().placeBid(id4, BidType.PASS, null);

            round.startPlayPhase(createFourPassBids(), soloBid, Suit.HEARTS, p1);

            for (int i = 0; i < 12; i++) round.finalizeTrick(mockCompletedTrick(id3));

            assertFalse(round.isFinished());
            assertEquals(p3, round.getCurrentPlayer());

            round.finalizeTrick(mockCompletedTrick(id3));
            assertTrue(round.isFinished());

            // SOLO failed: bidder p1 got 0 tricks. calculateBasePoints(0) = -75.
            // Score = -75 * 2 = -150; each of 3 opponents gains (-75 * 2 * -1) / 3 = +50.
            verify(p3, times(1)).updateScore(50);
        }

        @Test
        @DisplayName("isMiserieEarlyTermination instantly finishes round if all miserie bidders fail")
        void miserieEarlyTermination() {
            Bid miserieBid = round.getBidManager().placeBid(id1, BidType.MISERIE, null); // sole miserie bidder
            round.getBidManager().placeBid(id2, BidType.PASS, null);
            round.getBidManager().placeBid(id3, BidType.PASS, null);
            round.getBidManager().placeBid(id4, BidType.PASS, null);

            round.startPlayPhase(List.of(miserieBid, new PassBid(), new PassBid(), new PassBid()), miserieBid, null, p1);

            // P1 wins a trick → only miserie bidder fails → early termination
            round.finalizeTrick(mockCompletedTrick(id1));

            assertTrue(round.isFinished(),
                    "Round should early-terminate when the only Miserie bidder fails.");
        }
    }

    @Nested
    @DisplayName("Play Mode Scoring & Distribution")
    class PlayModeScoringTests {

        @Disabled("All real bid types have basePoints divisible by 3 — " +
                  "this guard protects against corrupt bids which cannot be created without mocking final records.")
        @Test
        @DisplayName("distributeScores throws if 1v3 game is not divisible by 3")
        void zeroSumValidation() {
            // Guard is defensive — cannot be triggered with any real BidType (all have points % 3 == 0).
        }

        @Test
        @DisplayName("getWinningPlayers returns defenders if standard bid fails")
        void getWinningPlayers_FailedStandardBid() {
            Bid soloBid = round.getBidManager().placeBid(id1, BidType.SOLO, Suit.HEARTS);
            round.getBidManager().placeBid(id2, BidType.PASS, null);
            round.getBidManager().placeBid(id3, BidType.PASS, null);
            round.getBidManager().placeBid(id4, BidType.PASS, null);

            // p2 wins all 13 tricks → bidder p1 fails (calculateBasePoints(0) = -75 < 0)
            round.startPlayPhase(createFourPassBids(), soloBid, Suit.HEARTS, p1);
            for (int i = 0; i < 13; i++) round.finalizeTrick(mockCompletedTrick(id2));

            List<Player> winners = round.getWinningPlayers();
            assertFalse(winners.contains(p1));
            assertTrue(winners.containsAll(List.of(p2, p3, p4)));
        }

        @Test
        @DisplayName("getWinningPlayers handles Miserie per-participant evaluation")
        void getWinningPlayers_Miserie() {
            // Two miserie bidders — team = {p1, p2}
            Bid miserieBid1 = round.getBidManager().placeBid(id1, BidType.MISERIE, null);
            round.getBidManager().placeBid(id2, BidType.MISERIE, null);
            round.getBidManager().placeBid(id3, BidType.PASS, null);
            round.getBidManager().placeBid(id4, BidType.PASS, null);

            round.startPlayPhase(createFourPassBids(), miserieBid1, null, p1);

            // p1 takes 0 tricks (succeeds), p2 takes 1 trick (fails)
            round.finalizeTrick(mockCompletedTrick(id2));
            for (int i = 0; i < 12; i++) round.finalizeTrick(mockCompletedTrick(id3));

            List<Player> winners = round.getWinningPlayers();
            assertTrue(winners.contains(p1));
            assertFalse(winners.contains(p2));
        }
    }

    @Nested
    @DisplayName("Snapshot Restoration")
    class SnapshotTests {

        @Test
        @DisplayName("restoreFromSnapshot validation guards")
        void restoreValidation() {
            Bid someBid = new PassBid();
            List<Player> parts = List.of(p1);
            List<Integer> deltas = List.of(0, 0, 0, 0);

            assertThrows(IllegalArgumentException.class, () -> round.restoreFromSnapshot(null, Suit.HEARTS, parts, 13, null, deltas));
            assertThrows(IllegalArgumentException.class, () -> round.restoreFromSnapshot(someBid, Suit.HEARTS, null, 13, null, deltas));
            // Note: domain does not validate that participants are members of the round
            assertThrows(IllegalArgumentException.class, () -> round.restoreFromSnapshot(someBid, Suit.HEARTS, parts, -2, null, deltas));
            assertThrows(IllegalArgumentException.class, () -> round.restoreFromSnapshot(someBid, Suit.HEARTS, parts, 14, null, deltas));
            assertThrows(IllegalArgumentException.class, () -> round.restoreFromSnapshot(someBid, Suit.HEARTS, parts, 13, null, List.of(0)));
            // Note: domain does not validate null elements inside restoredScoreDeltas list
            assertThrows(IllegalArgumentException.class, () -> round.restoreFromSnapshot(someBid, Suit.HEARTS, parts, 13, List.of(p2), deltas));
        }

        @Test
        @DisplayName("restoreFromSnapshot applies historical data properly")
        void restoreSuccess() {
            Bid someBid = new PassBid();
            round.restoreFromSnapshot(someBid, Suit.CLUBS, List.of(p1), 10, null, List.of(30, -10, -10, -10));

            assertTrue(round.isFinished());
            assertEquals(someBid, round.getHighestBid());
            assertEquals(Suit.CLUBS, round.getTrumpSuit());
            assertEquals(10, round.getCountTricksWon());
            assertEquals(List.of(30, -10, -10, -10), round.getScoreDeltas());
            // restoreFromSnapshot does NOT populate BidManager — GamePersistenceService
            // calls placeBid before restoreFromSnapshot. The unit test for Round in
            // isolation simply leaves the manager empty.
            assertTrue(round.getBidManager().getAllBids().isEmpty());
        }
    }

    @Nested
    @DisplayName("Getters & State Queries")
    class GetterTests {

        @Test
        @DisplayName("getPlayerById throws on missing ID")
        void getPlayerById() {
            assertEquals(p1, round.getPlayerById(id1));
            assertThrows(IllegalStateException.class, () -> round.getPlayerById(new PlayerId()));
        }

        @Test
        @DisplayName("getBiddingTeamTricksWon handles empty teams gracefully")
        void getBiddingTeamTricksWon() {
            assertEquals(-1, round.getBiddingTeamTricksWon(),
                    "Should return -1 if teams not resolved yet");
        }

        @Test
        @DisplayName("getLastPlayedTrick returns null if empty, getTricks works after abort")
        void getLastPlayedTrick() {
            assertNull(round.getLastPlayedTrick());

            round.abortWithAllPass(createFourPassBids());
            round.getTricks();
        }

        @Test
        @DisplayName("getWinningPlayers returns empty list if not finished")
        void getWinningPlayers_NotFinished() {
            assertTrue(round.getWinningPlayers().isEmpty());
        }

        @Test
        @DisplayName("setters, simple accessors, and BidManager pass-through")
        void coverageFillers() {
            Bid passBid = new PassBid();
            round.setHighestBid(passBid);
            assertEquals(passBid, round.getHighestBid());
            assertEquals(2, round.getMultiplier());
            assertTrue(round.getCountMiserieWinners().isEmpty());
            assertTrue(round.getBids().isEmpty(),
                    "getBids() now delegates to BidManager — empty before any placeBid()");
            assertTrue(round.getBiddingTeamPlayers().isEmpty());
            assertEquals(List.of(p1, p2, p3, p4), round.getPlayers());

            // getBids() reflects the BidManager once a bid is registered
            round.getBidManager().placeBid(id1, BidType.SOLO, Suit.HEARTS);
            assertEquals(1, round.getBids().size());
            assertEquals(BidType.SOLO, round.getBids().get(0).getType());
        }
    }

    // --- Helpers ---
    private List<Card> createDummyHand(int size) {
        List<Card> hand = new ArrayList<>();
        Suit[] suits = Suit.values();
        Rank[] ranks = Rank.values();
        for (int i = 0; i < size; i++) {
            hand.add(new Card(suits[i % suits.length], ranks[i % ranks.length]));
        }
        return hand;
    }

    private Trick mockCompletedTrick(PlayerId winnerId) {
        Trick mockTrick = mock(Trick.class);
        when(mockTrick.getTurns()).thenReturn(List.of(mock(), mock(), mock(), mock()));
        when(mockTrick.getWinningPlayerId()).thenReturn(winnerId);
        return mockTrick;
    }

    private List<Bid> createFourPassBids() {
        return List.of(new PassBid(), new PassBid(), new PassBid(), new PassBid());
    }
}
