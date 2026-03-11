package base.domain.deck;

import base.domain.card.Card;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DeckTest {

    private Deck deck;

    @BeforeEach
    void setUp() {
        deck = new Deck();
    }

    @Test
    void constructor_InitializesFullStandardDeck() {
        List<Card> cards = deck.getCards();

        // A standard deck must contain exactly 52 cards
        assertEquals(52, cards.size(), "Deck should contain exactly 52 cards.");

        // Using a Set to ensure every single card is entirely unique (no duplicates)
        Set<Card> uniqueCards = new HashSet<>(cards);
        assertEquals(52, uniqueCards.size(), "All 52 cards in the deck must be unique.");
    }

    @Test
    void deal_DistributesCardsCorrectlyToFourPlayers() {
        List<List<Card>> hands = deck.deal();

        // Ensure exactly 4 hands are created
        assertEquals(4, hands.size(), "The deck must deal exactly 4 hands.");

        Set<Card> dealtCards = new HashSet<>();

        for (List<Card> hand : hands) {
            // Ensure each player gets exactly 13 cards (4 + 4 + 5 pattern)
            assertEquals(13, hand.size(), "Each player must receive exactly 13 cards.");
            dealtCards.addAll(hand);
        }

        // Ensure no cards were duplicated or lost during the dealing process
        assertEquals(52, dealtCards.size(), "All 52 unique cards must be dealt across the 4 hands.");
    }

    @Test
    void getCards_ReturnsDefensiveCopy() {
        List<Card> externalList = deck.getCards();

        // Attempt to modify the retrieved list (e.g., removing a card)
        externalList.removeFirst();

        // Verify the external list was modified
        assertEquals(51, externalList.size());

        // Verify the internal state of the Deck remains strictly protected (Encapsulation/Information Hiding)
        assertEquals(52, deck.getCards().size(), "Modifying the returned list should not affect the internal deck.");
    }

    @Test
    void shuffle_RandomizesDeckOrder() {
        List<Card> beforeShuffle = deck.getCards();

        deck.shuffle();

        List<Card> afterShuffle = deck.getCards();

        // Ensure no cards are lost during the shuffle
        assertEquals(52, afterShuffle.size(), "Deck must still have 52 cards after shuffling.");

        // Ensure the deck contains the exact same cards, just in a different order
        assertTrue(afterShuffle.containsAll(beforeShuffle));
        assertTrue(beforeShuffle.containsAll(afterShuffle));

        // Note: With 52! (factorial) possible permutations, the chance of the deck
        // shuffling into the exact same order is statistically impossible.
        assertNotEquals(beforeShuffle, afterShuffle, "The order of the cards should change after shuffling.");
    }
}