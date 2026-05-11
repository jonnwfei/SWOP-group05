package base.domain.round;

import base.domain.bid.*;
import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.trick.Trick;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@DisplayName("Round Domain Entity Tests")
class RoundTest {

    @Mock private Player p1;
    @Mock private Player p2;
    @Mock private Player p3;
    @Mock private Player p4;
    @Mock private Player externalPlayer;

    @Mock private Bid mockHighestBid;

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
            List<Bid> validBids = List.of(mockHighestBid, mock(Bid.class), mock(Bid.class), mock(Bid.class));

            assertThrows(IllegalArgumentException.class, () -> round.startPlayPhase(null, mockHighestBid, Suit.HEARTS, p1));
            assertThrows(IllegalArgumentException.class, () -> round.startPlayPhase(validBids.subList(0, 3), mockHighestBid, Suit.HEARTS, p1));
            assertThrows(IllegalArgumentException.class, () -> round.startPlayPhase(validBids, null, Suit.HEARTS, p1));
            assertThrows(IllegalArgumentException.class, () -> round.startPlayPhase(validBids, mockHighestBid, Suit.HEARTS, null));
        }

        @Test
        @DisplayName("startPlayPhase resolves the bidding team via BidManager and locks in state")
        void startPlayPhase_Success() {
            // Proposal by p1, accepted by p2 — manager-resolved team = {p1, p2}
            Bid proposalBid = mock(Bid.class);
            when(proposalBid.getType()).thenReturn(BidType.PROPOSAL);
            when(mockHighestBid.getType()).thenReturn(BidType.ACCEPTANCE);

            round.getBidManager().placeBid(id1, proposalBid.getType(), null);
            round.getBidManager().placeBid(id2, mockHighestBid.getType(), null);

            List<Bid> bids = List.of(proposalBid, mockHighestBid, mock(Bid.class), mock(Bid.class));
            round.startPlayPhase(bids, mockHighestBid, Suit.SPADES, p3);

            assertEquals(mockHighestBid, round.getHighestBid());
            assertEquals(Suit.SPADES, round.getTrumpSuit());
            assertEquals(p3, round.getCurrentPlayer());
            assertEquals(2, round.getBiddingTeamPlayers().size());
            assertTrue(round.getBiddingTeamPlayers().containsAll(List.of(p1, p2)));
        }

        @Test
        @DisplayName("resolveTeams throws if cards do not sum to 52")
        void resolveTeams_HandValidation() {
            when(mockHighestBid.getType()).thenReturn(BidType.SOLO);
            round.getBidManager().placeBid(id1, mockHighestBid.getType(), null);

            List<Bid> bids = List.of(mockHighestBid, mock(Bid.class), mock(Bid.class), mock(Bid.class));
            when(p1.getHand()).thenReturn(createDummyHand(1)); // breaks the 52-card invariant

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> round.startPlayPhase(bids, mockHighestBid, Suit.SPADES, p3));
            assertTrue(ex.getMessage().contains("before the play phase begins"));
        }

        @Test
        @DisplayName("abortWithAllPass validation guards")
        void abortWithAllPass_Validation() {
            Bid passBid = mock(Bid.class);
            when(passBid.getType()).thenReturn(BidType.PASS);
            Bid soloBid = mock(Bid.class);
            when(soloBid.getType()).thenReturn(BidType.SOLO);

            assertThrows(IllegalArgumentException.class, () -> round.abortWithAllPass(null));
            assertThrows(IllegalArgumentException.class, () -> round.abortWithAllPass(List.of(passBid)));
            assertThrows(IllegalArgumentException.class,
                    () -> round.abortWithAllPass(List.of(passBid, passBid, passBid, soloBid)),
                    "All bids must be PASS");
        }

        @Test
        @DisplayName("abortWithAllPass flushes hands and ends round")
        void abortWithAllPass_Success() {
            Bid passBid = mock(Bid.class);
            when(passBid.getType()).thenReturn(BidType.PASS);
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
            when(mockHighestBid.getType()).thenReturn(BidType.SOLO);
            when(mockHighestBid.calculateBasePoints(anyInt())).thenReturn(30);
            round.getBidManager().placeBid(id1, mockHighestBid.getType(), Suit.HEARTS); // team = {id1}

            round.startPlayPhase(createFourPassBids(), mockHighestBid, Suit.HEARTS, p1);

            for (int i = 0; i < 12; i++) round.finalizeTrick(mockCompletedTrick(id3));

            assertFalse(round.isFinished());
            assertEquals(p3, round.getCurrentPlayer());

            round.finalizeTrick(mockCompletedTrick(id3));
            assertTrue(round.isFinished());

            // Score distributed once at end of round: 30 base * 2 multiplier = 60,
            // each opponent (incl. p3) pays a third: -20.
            verify(p3, times(1)).updateScore(-20);
        }

        @Test
        @DisplayName("isMiserieEarlyTermination instantly finishes round if all miserie bidders fail")
        void miserieEarlyTermination() {
            when(mockHighestBid.getType()).thenReturn(BidType.MISERIE);
            round.getBidManager().placeBid(id1, mockHighestBid.getType(), null); // sole miserie bidder

            List<Bid> bids = new ArrayList<>();
            bids.add(mockHighestBid);
            bids.add(mock(Bid.class)); bids.add(mock(Bid.class)); bids.add(mock(Bid.class));

            round.startPlayPhase(bids, mockHighestBid, null, p1);

            // P1 wins a trick → only miserie bidder fails → early termination
            round.finalizeTrick(mockCompletedTrick(id1));

            assertTrue(round.isFinished(),
                    "Round should early-terminate when the only Miserie bidder fails.");
        }
    }

    @Nested
    @DisplayName("Count Mode Scoring")
    class CountModeScoringTests {

        @Test
        @DisplayName("calculateScoresForCount validation guards")
        void calculateScoresForCount_Validation() {
            assertThrows(IllegalArgumentException.class, () -> round.calculateScoresForCount(null, 13, List.of(p1), null));
            assertThrows(IllegalArgumentException.class, () -> round.calculateScoresForCount(mockHighestBid, 13, null, null));
            assertThrows(IllegalArgumentException.class, () -> round.calculateScoresForCount(mockHighestBid, 13, Collections.emptyList(), null));
            assertThrows(IllegalArgumentException.class, () -> round.calculateScoresForCount(mockHighestBid, 13, Arrays.asList(p1, null), null));
            assertThrows(IllegalArgumentException.class, () -> round.calculateScoresForCount(mockHighestBid, 13, List.of(p1, p2, p3, p4, externalPlayer), null));

            when(mockHighestBid.getType()).thenReturn(BidType.SOLO);
            assertThrows(IllegalArgumentException.class, () -> round.calculateScoresForCount(mockHighestBid, -2, List.of(p1), null));
            assertThrows(IllegalArgumentException.class, () -> round.calculateScoresForCount(mockHighestBid, 14, List.of(p1), null));
        }

        @Test
        @DisplayName("Miserie count scoring uses base points per participant outcome")
        void countMiserieScoring() {
            when(mockHighestBid.getType()).thenReturn(BidType.MISERIE);
            when(mockHighestBid.calculateBasePoints(0)).thenReturn(30);   // succeeded
            when(mockHighestBid.calculateBasePoints(1)).thenReturn(-30);  // failed

            assertThrows(IllegalArgumentException.class,
                    () -> round.calculateScoresForCount(mockHighestBid, -1, List.of(p1), Arrays.asList(p1, null)));

            // p1 won the miserie, p2 lost it. Count mode passes the team/winners explicitly,
            // so no BidManager population is needed.
            round.calculateScoresForCount(mockHighestBid, -1, List.of(p1, p2), List.of(p1));

            assertTrue(round.isFinished());
            verify(p1).updateScore(60);  //  30 * multiplier(2)
            verify(p2).updateScore(-60); // -30 * multiplier(2)
        }

        @Test
        @DisplayName("Standard count scoring distributes points across teams")
        void countStandardScoring() {
            when(mockHighestBid.getType()).thenReturn(BidType.PROPOSAL);
            when(mockHighestBid.calculateBasePoints(13)).thenReturn(30);

            round.calculateScoresForCount(mockHighestBid, 13, List.of(p1, p2), null);

            verify(p1).updateScore(60);
            verify(p2).updateScore(60);
            verify(p3).updateScore(-60);
            verify(p4).updateScore(-60);
        }
    }

    @Nested
    @DisplayName("Play Mode Scoring & Distribution")
    class PlayModeScoringTests {

        @Test
        @DisplayName("distributeScores throws if 1v3 game is not divisible by 3")
        void zeroSumValidation() {
            when(mockHighestBid.getType()).thenReturn(BidType.SOLO);
            when(mockHighestBid.calculateBasePoints(anyInt())).thenReturn(10); // 10 % 3 != 0

            Round indivisibleRound = new Round(players, p1, 1);
            indivisibleRound.getBidManager().placeBid(id1, mockHighestBid.getType(), Suit.HEARTS);

            indivisibleRound.startPlayPhase(createFourPassBids(), mockHighestBid, Suit.HEARTS, p1);
            for (int i = 0; i < 12; i++) indivisibleRound.finalizeTrick(mockCompletedTrick(id1));

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> indivisibleRound.finalizeTrick(mockCompletedTrick(id1)));
            assertTrue(ex.getMessage().contains("divisible by 3"));
        }

        @Test
        @DisplayName("getWinningPlayers returns defenders if standard bid fails")
        void getWinningPlayers_FailedStandardBid() {
            when(mockHighestBid.getType()).thenReturn(BidType.SOLO);
            when(mockHighestBid.calculateBasePoints(0)).thenReturn(-30); // failed
            round.getBidManager().placeBid(id1, mockHighestBid.getType(), null);

            round.startPlayPhase(createFourPassBids(), mockHighestBid, Suit.HEARTS, p1);
            for (int i = 0; i < 13; i++) round.finalizeTrick(mockCompletedTrick(id2));

            List<Player> winners = round.getWinningPlayers();
            assertFalse(winners.contains(p1));
            assertTrue(winners.containsAll(List.of(p2, p3, p4)));
        }

        @Test
        @DisplayName("getWinningPlayers handles Miserie per-participant evaluation")
        void getWinningPlayers_Miserie() {
            when(mockHighestBid.getType()).thenReturn(BidType.MISERIE);
            when(mockHighestBid.calculateBasePoints(anyInt())).thenReturn(30);

            // Two miserie bidders → team = {id1, id2}
            Bid secondMiserie = mock(Bid.class);
            when(secondMiserie.getType()).thenReturn(BidType.MISERIE);
            when(secondMiserie.calculateBasePoints(anyInt())).thenReturn(30);
            round.getBidManager().placeBid(id1, mockHighestBid.getType(), null);
            round.getBidManager().placeBid(id2, secondMiserie.getType(), null);

            round.startPlayPhase(createFourPassBids(), mockHighestBid, null, p1);

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
            List<Player> parts = List.of(p1);
            List<Integer> deltas = List.of(0, 0, 0, 0);

            assertThrows(IllegalArgumentException.class, () -> round.restoreFromSnapshot(null, Suit.HEARTS, parts, 13, null, deltas));
            assertThrows(IllegalArgumentException.class, () -> round.restoreFromSnapshot(mockHighestBid, Suit.HEARTS, null, 13, null, deltas));
            assertThrows(IllegalArgumentException.class, () -> round.restoreFromSnapshot(mockHighestBid, Suit.HEARTS, List.of(externalPlayer), 13, null, deltas));
            assertThrows(IllegalArgumentException.class, () -> round.restoreFromSnapshot(mockHighestBid, Suit.HEARTS, parts, -2, null, deltas));
            assertThrows(IllegalArgumentException.class, () -> round.restoreFromSnapshot(mockHighestBid, Suit.HEARTS, parts, 14, null, deltas));
            assertThrows(IllegalArgumentException.class, () -> round.restoreFromSnapshot(mockHighestBid, Suit.HEARTS, parts, 13, null, List.of(0)));
            assertThrows(IllegalArgumentException.class, () -> round.restoreFromSnapshot(mockHighestBid, Suit.HEARTS, parts, 13, null, Arrays.asList(0, null, 0, 0)));
            assertThrows(IllegalArgumentException.class, () -> round.restoreFromSnapshot(mockHighestBid, Suit.HEARTS, parts, 13, List.of(p2), deltas));
        }

        @Test
        @DisplayName("restoreFromSnapshot applies historical data properly")
        void restoreSuccess() {
            round.restoreFromSnapshot(mockHighestBid, Suit.CLUBS, List.of(p1), 10, null, List.of(30, -10, -10, -10));

            assertTrue(round.isFinished());
            assertEquals(mockHighestBid, round.getHighestBid());
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
            round.setHighestBid(mockHighestBid);
            assertEquals(mockHighestBid, round.getHighestBid());
            assertEquals(2, round.getMultiplier());
            assertTrue(round.getCountMiserieWinners().isEmpty());
            assertTrue(round.getBids().isEmpty(),
                    "getBids() now delegates to BidManager — empty before any placeBid()");
            assertTrue(round.getBiddingTeamPlayers().isEmpty());
            assertEquals(List.of(p1, p2, p3, p4), round.getPlayers());

            // getBids() reflects the BidManager once a bid is registered
            when(mockHighestBid.getType()).thenReturn(BidType.SOLO);
            round.getBidManager().placeBid(id1, mockHighestBid.getType(), Suit.HEARTS);
            assertEquals(1, round.getBids().size());
            assertSame(mockHighestBid, round.getBids().get(0));
        }
    }

    // --- Helpers ---
    private List<Card> createDummyHand(int size) {
        List<Card> dummyList = new ArrayList<>();
        for (int i = 0; i < size; i++) dummyList.add(mock(Card.class));
        return dummyList;
    }

    private Trick mockCompletedTrick(PlayerId winnerId) {
        Trick mockTrick = mock(Trick.class);
        when(mockTrick.getTurns()).thenReturn(List.of(mock(), mock(), mock(), mock()));
        when(mockTrick.getWinningPlayerId()).thenReturn(winnerId);
        return mockTrick;
    }

    private List<Bid> createFourPassBids() {
        Bid passBid = mock(Bid.class);
        when(passBid.getType()).thenReturn(BidType.PASS);
        return List.of(passBid, passBid, passBid, passBid);
    }
}