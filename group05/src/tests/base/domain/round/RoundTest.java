package base.domain.round;

import base.domain.bid.Bid;
import base.domain.bid.BidCategory;
import base.domain.bid.BidType;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@DisplayName("Round Domain Entity Tests")
class RoundTest {

    @Mock private Player p1;
    @Mock private Player p2;
    @Mock private Player p3;
    @Mock private Player p4;

    @Mock private Bid mockHighestBid;

    private AutoCloseable mocks;
    private List<Player> players;
    private Round round;

    private final PlayerId id1 = new PlayerId("id-1");
    private final PlayerId id2 = new PlayerId("id-2");
    private final PlayerId id3 = new PlayerId("id-3");
    private final PlayerId id4 = new PlayerId("id-4");

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        // Stub player IDs so getPlayerById() works correctly
        when(p1.getId()).thenReturn(id1);
        when(p2.getId()).thenReturn(id2);
        when(p3.getId()).thenReturn(id3);
        when(p4.getId()).thenReturn(id4);

        // To avoid null pointers in totalCards check
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
            assertThat(round.getPlayers()).hasSize(4);
            assertThat(round.getCurrentPlayer()).isEqualTo(p1);
            assertThat(round.getMultiplier()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should reject invalid player lists or starting players")
        void shouldRejectInvalidSetups() {
            assertThatThrownBy(() -> new Round(List.of(p1, p2, p3), p1, 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exactly 4 players");

            assertThatThrownBy(() -> new Round(players, null, 1))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Bidding Phase Management")
    class BiddingPhaseTests {

        @Test
        @DisplayName("startPlayPhase() should lock in the contract and resolve teams")
        void shouldStartPlayPhase() {
            // Arrange
            List<Bid> finalBids = List.of(mockHighestBid, mock(Bid.class), mock(Bid.class), mock(Bid.class));

            // Mocking the Bid's internal logic to return P1 as the solo team
            when(mockHighestBid.getTeam(anyList(), anyList())).thenReturn(List.of(p1));

            // Mock exactly 52 cards across players to pass validation
            when(p1.getHand()).thenReturn(createDummyHand(13));
            when(p2.getHand()).thenReturn(createDummyHand(13));
            when(p3.getHand()).thenReturn(createDummyHand(13));
            when(p4.getHand()).thenReturn(createDummyHand(13));

            // Act
            round.startPlayPhase(finalBids, mockHighestBid, Suit.HEARTS, p2);

            // Assert
            assertThat(round.getHighestBid()).isEqualTo(mockHighestBid);
            assertThat(round.getTrumpSuit()).isEqualTo(Suit.HEARTS);
            assertThat(round.getCurrentPlayer()).isEqualTo(p2); // P2 was set as first to play
            assertThat(round.getBiddingTeamPlayers()).containsExactly(p1);
        }

        @Test
        @DisplayName("abortWithAllPass() should flush hands and finish the round")
        void shouldAbortWithPass() {
            // Arrange
            Bid passBid = mock(Bid.class);
            when(passBid.getType()).thenReturn(BidType.PASS);
            List<Bid> passBids = List.of(passBid, passBid, passBid, passBid);

            // Act
            round.abortWithAllPass(passBids);

            // Assert
            assertThat(round.isFinished()).isTrue();
            assertThat(round.getHighestBid().getType()).isEqualTo(BidType.PASS);
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
            assertThat(round.getCurrentPlayer()).isEqualTo(p1);

            round.advanceToNextPlayer();
            assertThat(round.getCurrentPlayer()).isEqualTo(p2);

            round.advanceToNextPlayer();
            round.advanceToNextPlayer();
            round.advanceToNextPlayer();

            // Should wrap back around
            assertThat(round.getCurrentPlayer()).isEqualTo(p1);
        }

        @Test
        @DisplayName("registerCompletedTrick() should assign the next turn to the trick winner")
        void shouldAssignTurnToWinner() {
            // Arrange
            Trick mockTrick = mock(Trick.class);
            when(mockTrick.getTurns()).thenReturn(List.of(mock(), mock(), mock(), mock())); // 4 turns = complete
            when(mockTrick.getWinningPlayerId()).thenReturn(id3); // P3 won

            // Act
            round.registerCompletedTrick(mockTrick);

            // Assert
            assertThat(round.getTricks()).hasSize(1);
            assertThat(round.getCurrentPlayer()).isEqualTo(p3);
        }

        @Test
        @DisplayName("isFinished() should return true for early Miserie termination")
        void shouldTerminateMiserieEarly() {
            // Arrange: Setup a Miserie bid
            when(mockHighestBid.getType()).thenReturn(BidType.MISERIE);
            when(mockHighestBid.getPlayerId()).thenReturn(id1);
            round.setHighestBid(mockHighestBid);

            // P1 bids Miserie
            Bid mockP1Bid = mock(Bid.class);
            when(mockP1Bid.getType()).thenReturn(BidType.MISERIE);
            when(mockP1Bid.getPlayerId()).thenReturn(id1);

            // Force the round state (normally done via startPlayPhase)
            round.abortWithAllPass(List.of(mockP1Bid, mock(Bid.class), mock(Bid.class), mock(Bid.class)));
            round.setHighestBid(mockP1Bid);

            // Act: P1 wins a trick (Failing their Miserie contract)
            Trick mockTrick = mockCompletedTrick(id1);
            round.registerCompletedTrick(mockTrick);

            // Assert: Round ends instantly
            assertThat(round.isFinished()).isTrue();
        }
    }

    @Nested
    @DisplayName("Score Calculation")
    class ScoringTests {

        @Test
        @DisplayName("calculateScoresForCount() should properly distribute Solo bid points (1v3)")
        void shouldCalculateCountScoresSolo() {
            // Arrange: 1v3 game where base points = 30
            when(mockHighestBid.getType()).thenReturn(BidType.SOLO);
            when(mockHighestBid.calculateBasePoints(anyInt())).thenReturn(30);

            // Act: P1 won 13 tricks
            round.calculateScoresForCount(mockHighestBid, 13, List.of(p1), null);

            // Assert: P1 gets 30. P2, P3, P4 lose 10 each (Zero-sum)
            verify(p1).updateScore(30);
            verify(p2).updateScore(-10);
            verify(p3).updateScore(-10);
            verify(p4).updateScore(-10);
        }

        @Test
        @DisplayName("calculateScoresForCount() should properly distribute Proposal bid points (2v2)")
        void shouldCalculateCountScoresProposal() {
            // Arrange: 2v2 game where base points = 30
            when(mockHighestBid.getType()).thenReturn(BidType.PROPOSAL);
            when(mockHighestBid.calculateBasePoints(anyInt())).thenReturn(30);

            // Act: P1 & P2 won 13 tricks
            round.calculateScoresForCount(mockHighestBid, 13, List.of(p1, p2), null);

            // Assert: P1 & P2 get 30. P3 & P4 lose 30. (Zero-sum: 60 - 60 = 0)
            verify(p1).updateScore(30);
            verify(p2).updateScore(30);
            verify(p3).updateScore(-30);
            verify(p4).updateScore(-30);
        }
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private Trick mockCompletedTrick(PlayerId winnerId) {
        Trick mockTrick = mock(Trick.class);
        when(mockTrick.getTurns()).thenReturn(List.of(mock(), mock(), mock(), mock()));
        when(mockTrick.getWinningPlayerId()).thenReturn(winnerId);
        return mockTrick;
    }

    private List<Object> createDummyHand(int size) {
        List<Object> dummyList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            dummyList.add(new Object());
        }
        return dummyList;
    }
}