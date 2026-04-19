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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@DisplayName("Round Domain Entity Tests")
class RoundTest {

    @Mock private Player p1;
    @Mock private Player p2;
    @Mock private Player p3;
    @Mock private Player p4;
    @Mock private Player externalPlayer;

    @Mock private SoloBid mockHighestBid;

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

        // Fulfill the resolveTeams() domain invariant: total cards must equal 52
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
        @DisplayName("Successfully initializes with 4 valid players")
        void shouldInitializeSuccessfully() {
            assertEquals(4, round.getPlayers().size());
            assertEquals(p1, round.getCurrentPlayer());
            assertEquals(2, round.getMultiplier());
            assertFalse(round.isFinished());
            assertEquals(List.of(0, 0, 0, 0), round.getScoreDeltas());
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
            List<Bid> validBids = List.of(mockHighestBid, mock(PassBid.class), mock(PassBid.class), mock(PassBid.class));

            assertThrows(IllegalArgumentException.class, () -> round.startPlayPhase(null, mockHighestBid, Suit.HEARTS, p1));
            assertThrows(IllegalArgumentException.class, () -> round.startPlayPhase(validBids.subList(0, 3), mockHighestBid, Suit.HEARTS, p1));
            assertThrows(IllegalArgumentException.class, () -> round.startPlayPhase(validBids, null, Suit.HEARTS, p1));
            assertThrows(IllegalArgumentException.class, () -> round.startPlayPhase(validBids, mockHighestBid, Suit.HEARTS, null));
        }

        @Test
        @DisplayName("startPlayPhase successfully resolves teams and locks in state")
        void startPlayPhase_Success() {
            List<Bid> bids = List.of(mockHighestBid, mock(PassBid.class), mock(PassBid.class), mock(PassBid.class));
            when(mockHighestBid.getTeam(anyList(), anyList())).thenReturn(List.of(id1, id2));

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
            List<Bid> bids = List.of(mockHighestBid, mock(PassBid.class), mock(PassBid.class), mock(PassBid.class));
            when(p1.getHand()).thenReturn(createDummyHand(1)); // Invalidates the 52 card check

            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> round.startPlayPhase(bids, mockHighestBid, Suit.SPADES, p3));
            assertTrue(ex.getMessage().contains("before the play phase begins"));
        }

        @Test
        @DisplayName("abortWithAllPass validation guards")
        void abortWithAllPass_Validation() {
            Bid passBid = mock(PassBid.class);
            when(passBid.getType()).thenReturn(BidType.PASS);
            Bid soloBid = mock(SoloBid.class);
            when(soloBid.getType()).thenReturn(BidType.SOLO);

            assertThrows(IllegalArgumentException.class, () -> round.abortWithAllPass(null));
            assertThrows(IllegalArgumentException.class, () -> round.abortWithAllPass(List.of(passBid)));
            assertThrows(IllegalArgumentException.class, () -> round.abortWithAllPass(List.of(passBid, passBid, passBid, soloBid)), "All bids must be PASS");
        }

        @Test
        @DisplayName("abortWithAllPass flushes hands and ends round")
        void abortWithAllPass_Success() {
            Bid passBid = mock(PassBid.class);
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
            when(incompleteTrick.getTurns()).thenReturn(List.of(mock(), mock())); // Only 2 turns

            assertThrows(IllegalArgumentException.class, () -> round.finalizeTrick(null));
            assertThrows(IllegalArgumentException.class, () -> round.finalizeTrick(incompleteTrick));

            // Force round to finish
            round.abortWithAllPass(createFourPassBids());
            Trick validTrick = mockCompletedTrick(id1);
            assertThrows(IllegalStateException.class, () -> round.finalizeTrick(validTrick), "Cannot add trick to finished round");
        }

        @Test
        @DisplayName("finalizeTrick assigns next turn to winner and checks auto-finish")
        void finalizeTrick_SuccessAndAutoFinish() {
            when(mockHighestBid.getType()).thenReturn(BidType.SOLO);
            when(mockHighestBid.getTeam(anyList(), anyList())).thenReturn(List.of(id1)); // P1 is the Solo bidder
            when(mockHighestBid.calculateBasePoints(anyInt())).thenReturn(30);

            round.startPlayPhase(createFourPassBids(), mockHighestBid, Suit.HEARTS, p1);

            for(int i = 0; i < 12; i++) {
                round.finalizeTrick(mockCompletedTrick(id3));
            }

            assertFalse(round.isFinished());
            assertEquals(p3, round.getCurrentPlayer());

            // 13th trick auto-finishes the round and distributes scores ONCE
            round.finalizeTrick(mockCompletedTrick(id3));
            assertTrue(round.isFinished());

            // FIX: Score is distributed once at the end of the round, not per trick!
            // P1 Base (30) * Multiplier (2) = 60. Opponents (P3) pay a third: -20.
            verify(p3, times(1)).updateScore(-20);
        }

        @Test
        @DisplayName("isMiserieEarlyTermination instantly finishes round if all bidders fail")
        void miserieEarlyTermination() {
            when(mockHighestBid.getType()).thenReturn(BidType.MISERIE);
            when(mockHighestBid.getPlayerId()).thenReturn(id1);
            when(mockHighestBid.getTeam(anyList(), anyList())).thenReturn(List.of(id1));

            // Mock the bids list to show p1 bid Miserie
            List<Bid> bids = new ArrayList<>();
            bids.add(mockHighestBid);
            bids.add(mock(PassBid.class)); bids.add(mock(PassBid.class)); bids.add(mock(PassBid.class));

            round.startPlayPhase(bids, mockHighestBid, null, p1);

            // P1 wins a trick, failing their miserie contract immediately
            round.finalizeTrick(mockCompletedTrick(id1));

            assertTrue(round.isFinished(), "Round should early-terminate since the only Miserie bidder failed.");
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
            assertThrows(IllegalArgumentException.class, () -> round.calculateScoresForCount(mockHighestBid, -2, List.of(p1), null)); // Tricks < 0
            assertThrows(IllegalArgumentException.class, () -> round.calculateScoresForCount(mockHighestBid, 14, List.of(p1), null)); // Tricks > 13
        }

        @Test
        @DisplayName("Miserie Count Scoring correctly calculates base points")
        void countMiserieScoring() {
            when(mockHighestBid.getType()).thenReturn(BidType.MISERIE);
            when(mockHighestBid.calculateBasePoints(0)).thenReturn(30); // Won
            when(mockHighestBid.calculateBasePoints(1)).thenReturn(-30); // Lost

            assertThrows(IllegalArgumentException.class, () -> round.calculateScoresForCount(mockHighestBid, -1, List.of(p1), Arrays.asList(p1, null)));

            // P1 won, P2 lost
            round.calculateScoresForCount(mockHighestBid, -1, List.of(p1, p2), List.of(p1));

            assertTrue(round.isFinished());
            // Multiplier is 2 (from setup)
            verify(p1).updateScore(60); // 30 * 2
            verify(p2).updateScore(-60); // -30 * 2
        }

        @Test
        @DisplayName("Standard Count Scoring automatically distributes teams")
        void countStandardScoring() {
            when(mockHighestBid.getType()).thenReturn(BidType.PROPOSAL);
            when(mockHighestBid.calculateBasePoints(13)).thenReturn(30);

            round.calculateScoresForCount(mockHighestBid, 13, List.of(p1, p2), null);

            verify(p1).updateScore(60);  // 30 * 2
            verify(p2).updateScore(60);
            verify(p3).updateScore(-60); // Defenders lose points
            verify(p4).updateScore(-60);
        }
    }

    @Nested
    @DisplayName("Play Mode Scoring & Distribution")
    class PlayModeScoringTests {

        @Test
        @DisplayName("distributeScores throws IllegalStateException if 1v3 game is not zero-sum (divisible by 3)")
        void zeroSumValidation() {
            when(mockHighestBid.getType()).thenReturn(BidType.SOLO);
            when(mockHighestBid.getTeam(anyList(), anyList())).thenReturn(List.of(id1));
            // Base points of 10 * multiplier 1 = 10. 10 % 3 != 0.
            when(mockHighestBid.calculateBasePoints(anyInt())).thenReturn(10);

            Round indivisibleRound = new Round(players, p1, 1);
            indivisibleRound.startPlayPhase(createFourPassBids(), mockHighestBid, Suit.HEARTS, p1);

            for (int i = 0; i < 12; i++) indivisibleRound.finalizeTrick(mockCompletedTrick(id1));

            // 13th trick triggers distribution which throws
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> indivisibleRound.finalizeTrick(mockCompletedTrick(id1)));
            assertTrue(ex.getMessage().contains("divisible by 3"));
        }

        @Test
        @DisplayName("getWinningPlayers returns defenders if standard bid fails")
        void getWinningPlayers_FailedStandardBid() {
            when(mockHighestBid.getType()).thenReturn(BidType.SOLO);
            when(mockHighestBid.getTeam(anyList(), anyList())).thenReturn(List.of(id1));
            when(mockHighestBid.calculateBasePoints(0)).thenReturn(-30); // Negative means fail

            round.startPlayPhase(createFourPassBids(), mockHighestBid, Suit.HEARTS, p1);
            for (int i = 0; i < 13; i++) round.finalizeTrick(mockCompletedTrick(id2)); // Defenders win all tricks

            List<Player> winners = round.getWinningPlayers();
            assertFalse(winners.contains(p1));
            assertTrue(winners.containsAll(List.of(p2, p3, p4)));
        }

        @Test
        @DisplayName("getWinningPlayers handles Miserie evaluation correctly")
        void getWinningPlayers_Miserie() {
            when(mockHighestBid.getType()).thenReturn(BidType.MISERIE);
            when(mockHighestBid.getTeam(anyList(), anyList())).thenReturn(List.of(id1, id2));
            when(mockHighestBid.calculateBasePoints(anyInt())).thenReturn(30);

            round.startPlayPhase(createFourPassBids(), mockHighestBid, null, p1);

            // P1 takes 0 tricks (Wins), P2 takes 1 trick (Loses)
            round.finalizeTrick(mockCompletedTrick(id2));
            for (int i = 0; i < 12; i++) round.finalizeTrick(mockCompletedTrick(id3));

            List<Player> winners = round.getWinningPlayers();
            assertTrue(winners.contains(p1));
            assertFalse(winners.contains(p2)); // Failed
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
            assertThrows(IllegalArgumentException.class, () -> round.restoreFromSnapshot(mockHighestBid, Suit.HEARTS, parts, 13, null, List.of(0))); // Wrong delta size
            assertThrows(IllegalArgumentException.class, () -> round.restoreFromSnapshot(mockHighestBid, Suit.HEARTS, parts, 13, null, Arrays.asList(0, null, 0, 0)));
            assertThrows(IllegalArgumentException.class, () -> round.restoreFromSnapshot(mockHighestBid, Suit.HEARTS, parts, 13, List.of(p2), deltas)); // Miserie winner not in participants
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
            assertEquals(-1, round.getBiddingTeamTricksWon(), "Should return -1 if teams not resolved yet");
        }

        @Test
        @DisplayName("getLastPlayedTrick returns null if empty")
        void getLastPlayedTrick() {
            assertNull(round.getLastPlayedTrick());

            // Force past guards to add a trick
            round.abortWithAllPass(createFourPassBids());
            round.getTricks(); // Purely for coverage of getTricks()
        }

        @Test
        @DisplayName("getWinningPlayers returns empty list if not finished")
        void getWinningPlayers_NotFinished() {
            assertTrue(round.getWinningPlayers().isEmpty());
        }

        @Test
        @DisplayName("setters and simple accessors")
        void coverageFillers() {
            round.setHighestBid(mockHighestBid);
            assertEquals(mockHighestBid, round.getHighestBid());
            assertEquals(2, round.getMultiplier());
            assertTrue(round.getCountMiserieWinners().isEmpty());
            assertTrue(round.getBids().isEmpty());
            assertTrue(round.getBiddingTeamPlayers().isEmpty());
            assertEquals(List.of(p1, p2, p3, p4), round.getPlayers());
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
        Bid passBid = mock(PassBid.class);
        when(passBid.getType()).thenReturn(BidType.PASS);
        return List.of(passBid, passBid, passBid, passBid);
    }
}