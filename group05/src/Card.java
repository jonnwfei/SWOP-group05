import java.util.Objects;

enum Suit {
    CLUBS, DIAMONDS, HEARTS, SPADES
}

enum Value {
    TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN,
    JACK, QUEEN, KING, ACE

}

/**
 * @author John Cai
 */
public class Card {
    private final Suit suit;
    private final Value value;

    /**
     * @param suit of the card e.g. CLUBS, HEARTS, ...
     * @param value of the card e.g. TWO, THREE, ..., QUEEN, ACE
     * @throws IllegalArgumentException in case suit or value are null
     */
    public Card(Suit suit, Value value) {
        if (suit == null || value == null) {
            throw new IllegalArgumentException();
        }
        this.suit = suit;
        this.value = value;
    }

    /**
     * @return suit
     */
    public Suit getSuit() {
        return suit;
    }

    /**
     *
     * @return rank
     */
    public Value getRank() {
        return value;
    }
}