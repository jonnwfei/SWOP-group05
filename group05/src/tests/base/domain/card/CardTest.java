package base.domain.card;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Card Value Object")
class CardTest {

    @Nested
    @DisplayName("Constructor & Validation")
    class ConstructorTests {

        @Test
        @DisplayName("Successfully creates a card with valid parameters")
        void shouldCreateValidCard() {
            Card card = new Card(Suit.HEARTS, Rank.ACE);

            assertEquals(Suit.HEARTS, card.suit());
            assertEquals(Rank.ACE, card.rank());
        }

        @Test
        @DisplayName("Rejects instantiation if Suit is null")
        void shouldRejectNullSuit() {
            assertThrows(IllegalArgumentException.class, () -> new Card(null, Rank.ACE));
        }

        @Test
        @DisplayName("Rejects instantiation if Rank is null")
        void shouldRejectNullRank() {
            assertThrows(IllegalArgumentException.class, () -> new Card(Suit.SPADES, null));
        }
    }

    @Nested
    @DisplayName("Behavior & Overrides")
    class BehaviorTests {

        @Test
        @DisplayName("toString() formats correctly as 'RANK of SUIT'")
        void shouldFormatToStringCorrectly() {
            Card card = new Card(Suit.CLUBS, Rank.QUEEN);

            assertEquals("QUEEN of CLUBS", card.toString());
        }

        @Test
        @DisplayName("Records handle equality and hash codes by value, not memory reference")
        void shouldEvaluateEqualityByValue() {
            Card card1 = new Card(Suit.DIAMONDS, Rank.TEN);
            Card card2 = new Card(Suit.DIAMONDS, Rank.TEN);

            Card differentSuit = new Card(Suit.HEARTS, Rank.TEN);
            Card differentRank = new Card(Suit.DIAMONDS, Rank.NINE);

            assertEquals(card1, card2);
            assertEquals(card1.hashCode(), card2.hashCode());

            assertNotEquals(card1, differentSuit);
            assertNotEquals(card1, differentRank);
        }
    }
}