enum Suit {
    CLUBS, DIAMONDS, HEARTS, SPADES
}

enum Rank {
    TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN,
    JACK, QUEEN, KING, ACE

}

/**
 * @author John Cai
 * @since 24-02-2026
 */
public class Card {
    private final Suit suit;
    private final Rank rank;

    /**
     * @param suit of the card e.g. CLUBS, HEARTS, ...
     * @param rank of the card e.g. TWO, THREE, ..., QUEEN, ACE
     * @throws IllegalArgumentException in case suit or value are null
     */
    public Card(Suit suit, Rank rank) {
        if (suit == null || rank == null) {
            throw new IllegalArgumentException();
        }
        this.suit = suit;
        this.rank = rank;
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
    public Rank getRank() {
        return rank;
    }
}