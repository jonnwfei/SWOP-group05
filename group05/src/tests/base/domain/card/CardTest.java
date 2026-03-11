package base.domain.card;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CardTest {

    @Test
    void constructor_ValidInputs_CreatesCard() {
        Card card = new Card(Suit.HEARTS, Rank.ACE);

        assertEquals(Suit.HEARTS, card.suit());
        assertEquals(Rank.ACE, card.rank());
    }

    @Test
    void constructor_NullSuit_ThrowsException() {
        // Enforce the Class Invariant: A card cannot exist without a suit
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new Card(null, Rank.ACE)
        );
        assertNotNull(exception);
    }

    @Test
    void constructor_NullRank_ThrowsException() {
        // Enforce the Class Invariant: A card cannot exist without a rank
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new Card(Suit.SPADES, null)
        );
        assertNotNull(exception);
    }

    @Test
    void toString_ReturnsCorrectlyFormattedString() {
        Card card = new Card(Suit.CLUBS, Rank.QUEEN);

        // Verifies the custom toString() format
        assertEquals("QUEEN of CLUBS", card.toString());
    }

    @Test
    void testRecordEquality() {
        // Records give us equals() and hashCode() for free based on their fields.
        // Good practice to prove it works as expected for domain objects!
        Card card1 = new Card(Suit.DIAMONDS, Rank.TEN);
        Card card2 = new Card(Suit.DIAMONDS, Rank.TEN);
        Card card3 = new Card(Suit.HEARTS, Rank.TEN);

        assertEquals(card1, card2);
        assertNotEquals(card1, card3);
    }
}