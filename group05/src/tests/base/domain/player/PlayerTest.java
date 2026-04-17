package base.domain.player;

import base.domain.bid.Bid;
import base.domain.bid.SoloBid;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.strategy.HumanStrategy; // Using a concrete implementation
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("Player Entity Rules & State")
class PlayerTest {

    @Mock
    private HumanStrategy mockStrategy; // Mocking the class avoids sealed interface restrictions

    @Mock
    private SoloBid mockBid; // Mock a concrete class instead of the sealed 'Bid' interface

    private AutoCloseable mocks;
    private Player player;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        // HumanStrategy implements Strategy, so this is type-safe
        player = new Player(mockStrategy, "Alice");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @Nested
    @DisplayName("Constructor & Initialization")
    class ConstructorTests {

        @Test
        @DisplayName("Should initialize player with a unique ID and empty state")
        void createsPlayerSuccessfully() {
            assertThat(player.getId()).isNotNull();
            assertThat(player.getName()).isEqualTo("Alice");
            assertThat(player.getScore()).isZero();
            assertThat(player.getHand()).isEmpty();
            assertThat(player.getDecisionStrategy()).isEqualTo(mockStrategy);
        }

        @Test
        @DisplayName("Should reject null strategy or name")
        void throwsOnNullInputs() {
            assertThatThrownBy(() -> new Player(null, "Alice"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new Player(mockStrategy, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Hand Management")
    class HandManagementTests {

        @Test
        @DisplayName("setHand() should clear old cards and apply new sorted list")
        void setsAndSortsHand() {
            Card lowHeart = new Card(Suit.HEARTS, Rank.TWO);
            Card highHeart = new Card(Suit.HEARTS, Rank.ACE);
            Card club = new Card(Suit.CLUBS, Rank.KING);
            Card spade = new Card(Suit.SPADES, Rank.TEN);

            player.setHand(List.of(spade, lowHeart, club, highHeart));
            List<Card> hand = player.getHand();

            // Verifying Whist Sorting: Clubs -> Hearts (Ace then 2) -> Spades
            assertThat(hand).containsExactly(club, highHeart, lowHeart, spade);
        }

        @Test
        @DisplayName("getHand() should return a defensive copy to prevent tampering")
        void getHandIsDefensive() {
            player.setHand(List.of(new Card(Suit.HEARTS, Rank.TEN)));

            List<Card> externalHand = player.getHand();
            externalHand.clear();

            assertThat(player.getHand()).isNotEmpty();
        }

        @Test
        @DisplayName("removeCard() should update hand or throw if card is missing")
        void removesCard() {
            Card heartTen = new Card(Suit.HEARTS, Rank.TEN);
            player.setHand(List.of(heartTen));

            player.removeCard(heartTen);
            assertThat(player.getHand()).isEmpty();

            assertThatThrownBy(() -> player.removeCard(heartTen))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not in player hand");
        }
    }

    @Nested
    @DisplayName("Strategy Delegation")
    class StrategyDelegationTests {

        @Test
        @DisplayName("chooseCard() should delegate decision to strategy")
        void delegatesChooseCard() {
            Card expected = new Card(Suit.HEARTS, Rank.TEN);
            when(mockStrategy.chooseCardToPlay(anyList(), eq(Suit.HEARTS))).thenReturn(expected);

            Card result = player.chooseCard(Suit.HEARTS);

            assertThat(result).isEqualTo(expected);
            verify(mockStrategy).chooseCardToPlay(anyList(), eq(Suit.HEARTS));
        }

        @Test
        @DisplayName("chooseBid() should delegate decision to strategy")
        void delegatesChooseBid() {
            when(mockStrategy.determineBid(eq(player.getId()), anyList())).thenReturn(mockBid);

            Bid result = player.chooseBid();

            assertThat(result).isEqualTo(mockBid);
            verify(mockStrategy).determineBid(eq(player.getId()), anyList());
        }
    }

    @Nested
    @DisplayName("Score Tracking")
    class ScoreTests {

        @Test
        @DisplayName("updateScore() should accumulate both positive and negative deltas")
        void updatesScore() {
            player.updateScore(15);
            player.updateScore(-5);
            assertThat(player.getScore()).isEqualTo(10);
        }
    }
}