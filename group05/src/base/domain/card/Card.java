package base.domain.card;

/**
 * The type Card.
 *
 * @author John Cai
 * @since 24-02-2026
 */
public record Card(Suit suit, Rank rank) {
    /**
     * Instantiates a new Card
     *
     * @param suit of the card e.g. CLUBS, HEARTS, ...
     * @param rank of the card e.g. TWO, THREE, ..., QUEEN, ACE
     * @throws IllegalArgumentException in case suit or value are null
     */
    public Card {
        if (suit == null || rank == null) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Returns the stringified card
     *
     * @return Stringified card
     */
    @Override
    public String toString() {
        return this.rank +  " of " +  this.suit;
    }
}