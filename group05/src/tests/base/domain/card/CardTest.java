package base.domain.card;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Card Value Object")
class CardTest {

    @Nested
    @DisplayName("Constructor & Validation")
    class ConstructorTests {

        @Test
        @DisplayName("Successfully creates a card with valid parameters")
        void shouldCreateValidCard() {
            Card card = new Card(Suit.HEARTS, Rank.ACE);

            assertThat(card.suit()).isEqualTo(Suit.HEARTS);
            assertThat(card.rank()).isEqualTo(Rank.ACE);
        }

        @Test
        @DisplayName("Rejects instantiation if Suit is null")
        void shouldRejectNullSuit() {
            assertThatThrownBy(() -> new Card(null, Rank.ACE))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Rejects instantiation if Rank is null")
        void shouldRejectNullRank() {
            assertThatThrownBy(() -> new Card(Suit.SPADES, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Behavior & Overrides")
    class BehaviorTests {

        @Test
        @DisplayName("toString() formats correctly as 'RANK of SUIT'")
        void shouldFormatToStringCorrectly() {
            Card card = new Card(Suit.CLUBS, Rank.QUEEN);

            assertThat(card.toString()).isEqualTo("QUEEN of CLUBS");
        }

        @Test
        @DisplayName("Records handle equality and hash codes by value, not memory reference")
        void shouldEvaluateEqualityByValue() {
            Card card1 = new Card(Suit.DIAMONDS, Rank.TEN);
            Card card2 = new Card(Suit.DIAMONDS, Rank.TEN);

            Card differentSuit = new Card(Suit.HEARTS, Rank.TEN);
            Card differentRank = new Card(Suit.DIAMONDS, Rank.NINE);

            // AssertJ allows us to chain these value checks beautifully
            assertThat(card1)
                    .isEqualTo(card2)
                    .hasSameHashCodeAs(card2)
                    .isNotEqualTo(differentSuit)
                    .isNotEqualTo(differentRank);
        }
    }
}