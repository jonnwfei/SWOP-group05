package base.domain.round;

import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.bid.SoloBid;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Scenario tests for the Round domain entity[cite: 53].
 * Verifies turn mechanics, trick registration, and score distribution for Whist[cite: 148, 150].
 */
@DisplayName("Round Domain Entity Tests")
class RoundTest {

    @Mock private Player p1;
    @Mock private Player p2;
    @Mock private Player p3;
    @Mock private Player p4;

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

        when(p1.getId()).thenReturn(id1);
        when(p2.getId()).thenReturn(id2);
        when(p3.getId()).thenReturn(id3);
        when(p4.getId()).thenReturn(id4);

        lenient().when(p1.getHand()).thenReturn(new ArrayList<>(13));
        lenient().when(p2.getHand()).thenReturn(new ArrayList<>(13));
        lenient().when(p3.getHand()).thenReturn(new ArrayList<>(13));
        lenient().when(p4.getHand()).thenReturn(new ArrayList<>(13));

        players = List.of(p1, p2, p3, p4);
        round = new Round(players, p1, 1);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) mocks.close();
    }

    @Nested
    @DisplayName("Constructor & Initialization")
    class ConstructorTests {

        @Test
        @DisplayName("Should initialize round successfully with 4 players")
        void shouldInitializeSuccessfully() {
            assertEquals(4, round.getPlayers().size());
            assertEquals(p1, round.getCurrentPlayer());
            assertEquals(1, round.getMultiplier());
        }

        @Test
        @DisplayName("Should reject invalid player lists or starting players")
        void shouldRejectInvalidSetups() {
            // Negative scenario testing for illegal input [cite: 56]
            Exception e1 = assertThrows(IllegalArgumentException.class,
                    () -> new Round(List.of(p1, p2, p3), p1, 1));
            assertTrue(e1.getMessage().contains("exactly 4 players"));

            assertThrows(IllegalArgumentException.class,
                    () -> new Round(players, null, 1));
        }
    }

    @Nested
    @DisplayName("Bidding Phase Management")
    class BiddingPhaseTests {

        @Test
        @DisplayName("startPlayPhase() should lock in the contract and resolve teams")
        void shouldStartPlayPhase() {
            List<Bid> finalBids = List.of(mockHighestBid, mock(SoloBid.class), mock(SoloBid.class), mock(SoloBid.class));
            when(mockHighestBid.getTeam(anyList(), anyList())).thenReturn(List.of(id1));

            when(p1.getHand()).thenReturn(createDummyHand(13));
            when(p2.getHand()).thenReturn(createDummyHand(13));
            when(p3.getHand()).thenReturn(createDummyHand(13));
            when(p4.getHand()).thenReturn(createDummyHand(13));

            round.startPlayPhase(finalBids, mockHighestBid, Suit.HEARTS, p2);

            assertEquals(mockHighestBid, round.getHighestBid());
            assertEquals(Suit.HEARTS, round.getTrumpSuit());
            assertEquals(p2, round.getCurrentPlayer());
            assertEquals(1, round.getBiddingTeamPlayers().size());
            assertTrue(round.getBiddingTeamPlayers().contains(p1));
        }

        @Test
        @DisplayName("abortWithAllPass() should flush hands and finish the round")
        void shouldAbortWithPass() {
            SoloBid passBid = mock(SoloBid.class);
            when(passBid.getType()).thenReturn(BidType.PASS);
            List<Bid> passBids = List.of(passBid, passBid, passBid, passBid);

            round.abortWithAllPass(passBids);

            assertTrue(round.isFinished());
            assertEquals(BidType.PASS, round.getHighestBid().getType());
            verify(p1).flushHand();
            verify(p2).flushHand();
        }
    }

    @Nested
    @DisplayName("Play Phase & Turn Mechanics")
    class PlayPhaseTests {

        @Test
        @DisplayName("advanceToNextPlayer() should loop through players correctly")
        void shouldAdvanceTurns() {
            assertEquals(p1, round.getCurrentPlayer());

            round.advanceToNextPlayer();
            assertEquals(p2, round.getCurrentPlayer());

            round.advanceToNextPlayer();
            round.advanceToNextPlayer();
            round.advanceToNextPlayer();

            assertEquals(p1, round.getCurrentPlayer());
        }

        @Test
        @DisplayName("finalizeTrick() should assign the next turn to the trick winner")
        void shouldAssignTurnToWinner() {
            // 1. Arrange: Properly initialize the play phase
            when(mockHighestBid.getType()).thenReturn(BidType.SOLO);
            when(mockHighestBid.getTeam(anyList(), anyList())).thenReturn(List.of(id1));

            // FIX: Mock exactly 52 cards across players to pass validation
            when(p1.getHand()).thenReturn(createDummyHand(13));
            when(p2.getHand()).thenReturn(createDummyHand(13));
            when(p3.getHand()).thenReturn(createDummyHand(13));
            when(p4.getHand()).thenReturn(createDummyHand(13));

            List<Bid> dummyBids = List.of(mockHighestBid, mock(SoloBid.class), mock(SoloBid.class), mock(SoloBid.class));
            round.startPlayPhase(dummyBids, mockHighestBid, Suit.HEARTS, p1);

            Trick mockTrick = mock(Trick.class);
            when(mockTrick.getTurns()).thenReturn(List.of(mock(), mock(), mock(), mock()));
            when(mockTrick.getWinningPlayerId()).thenReturn(id3); // P3 won

            // 2. Act
            round.finalizeTrick(mockTrick);

            // 3. Assert
            assertEquals(1, round.getTricks().size());
            assertEquals(p3, round.getCurrentPlayer());
        }

        @Test
        @DisplayName("isFinished() should return true for early Miserie termination")
        void shouldTerminateMiserieEarly() {
            // 1. Arrange: Setup a Miserie bid
            when(mockHighestBid.getType()).thenReturn(BidType.MISERIE);
            when(mockHighestBid.getPlayerId()).thenReturn(id1);
            when(mockHighestBid.getTeam(anyList(), anyList())).thenReturn(List.of(id1));

            // FIX: Mock exactly 52 cards across players to pass validation
            when(p1.getHand()).thenReturn(createDummyHand(13));
            when(p2.getHand()).thenReturn(createDummyHand(13));
            when(p3.getHand()).thenReturn(createDummyHand(13));
            when(p4.getHand()).thenReturn(createDummyHand(13));

            List<Bid> finalBids = List.of(mockHighestBid, mock(SoloBid.class), mock(SoloBid.class), mock(SoloBid.class));
            round.startPlayPhase(finalBids, mockHighestBid, null, p1);

            // 2. Act: P1 wins a trick (Failing their Miserie contract)
            Trick mockTrick = mockCompletedTrick(id1);
            round.finalizeTrick(mockTrick);

            // 3. Assert: Round ends instantly
            assertTrue(round.isFinished());
        }
    }

    @Nested
    @DisplayName("Score Calculation")
    class ScoringTests {

        @Test
        @DisplayName("calculateScoresForCount() should properly distribute Solo bid points (1v3)")
        void shouldCalculateCountScoresSolo() {
            when(mockHighestBid.getType()).thenReturn(BidType.SOLO);
            when(mockHighestBid.calculateBasePoints(anyInt())).thenReturn(30);

            round.calculateScoresForCount(mockHighestBid, 13, List.of(p1), null);

            verify(p1).updateScore(30);
            verify(p2).updateScore(-10); // Points are zero-sum [cite: 206, 210, 212]
            verify(p3).updateScore(-10);
            verify(p4).updateScore(-10);
        }

        @Test
        @DisplayName("calculateScoresForCount() should properly distribute Proposal bid points (2v2)")
        void shouldCalculateCountScoresProposal() {
            when(mockHighestBid.getType()).thenReturn(BidType.PROPOSAL);
            when(mockHighestBid.calculateBasePoints(anyInt())).thenReturn(30);

            round.calculateScoresForCount(mockHighestBid, 13, List.of(p1, p2), null);

            verify(p1).updateScore(30);
            verify(p2).updateScore(30);
            verify(p3).updateScore(-30); // Pairs pay equal amounts [cite: 211]
            verify(p4).updateScore(-30);
        }

        @Test
        @DisplayName("restoreFromSnapshot() should mark a non-miserie round as finished and resolve winners")
        void shouldRestoreFinishedRoundAndResolveWinnersFromCountData() {
            when(mockHighestBid.getType()).thenReturn(BidType.SOLO);
            when(mockHighestBid.calculateBasePoints(9)).thenReturn(30);

            round.restoreFromSnapshot(
                    mockHighestBid,
                    Suit.HEARTS,
                    List.of(p1),
                    9,
                    List.of(),
                    List.of(30, -10, -10, -10)
            );

            assertTrue(round.isFinished());
            assertEquals(List.of(p1), round.getWinningPlayers());
        }

        @Test
        @DisplayName("restoreFromSnapshot() should use persisted miserie winners when trick history is absent")
        void shouldRestoreMiserieWinnersWithoutTricks() {
            when(mockHighestBid.getType()).thenReturn(BidType.MISERIE);

            round.restoreFromSnapshot(
                    mockHighestBid,
                    null,
                    List.of(p1, p2),
                    -1,
                    List.of(p2),
                    List.of(-15, 15, 0, 0)
            );

            assertTrue(round.isFinished());
            assertEquals(List.of(p2), round.getWinningPlayers());
        }
    }

    private Trick mockCompletedTrick(PlayerId winnerId) {
        Trick mockTrick = mock(Trick.class);
        when(mockTrick.getTurns()).thenReturn(List.of(mock(), mock(), mock(), mock()));
        when(mockTrick.getWinningPlayerId()).thenReturn(winnerId);
        return mockTrick;
    }

    private List<Card> createDummyHand(int size) {
        List<Card> dummyList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            dummyList.add(mock(Card.class));
        }
        return dummyList;
    }
}