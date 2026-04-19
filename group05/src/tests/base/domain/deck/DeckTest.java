package base.domain.deck;

import base.domain.card.Card;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Standard 52-Card Deck")
class DeckTest {

    private Deck deck;

    @BeforeEach
    void setUp() {
        deck = new Deck();
    }

    @Nested
    @DisplayName("Deck Initialization")
    class InitializationTests {

        @Test
        @DisplayName("Should initialize with exactly 52 unique cards")
        void constructor_InitializesFullStandardDeck() {
            List<Card> cards = deck.getCards();

            // Assert size and absence of nulls
            assertEquals(52, cards.size());
            assertFalse(cards.contains(null));

            // Assert all cards are unique
            Set<Card> uniqueCards = new HashSet<>(cards);
            assertEquals(52, uniqueCards.size());
        }
    }

    @Nested
    @DisplayName("Dealing Logic")
    class DealingTests {

        @Test
        @DisplayName("Should distribute 52 cards into four hands of 13 cards each")
        void deal_DistributesCardsCorrectlyToFourPlayers() {
            List<List<Card>> hands = deck.deal(Deck.DEAL_TYPE.WHIST);

            assertEquals(4, hands.size());

            // Flatten all hands to check the total pool of dealt cards
            List<Card> allDealtCards = hands.stream()
                    .flatMap(List::stream)
                    .toList();

            // Assert exact amount of cards was dealt
            assertEquals(52, allDealtCards.size());

            // Assert no duplicates across all dealt hands
            Set<Card> uniqueDealtCards = new HashSet<>(allDealtCards);
            assertEquals(52, uniqueDealtCards.size());

            for (List<Card> hand : hands) {
                assertEquals(13, hand.size());
            }
        }
    }

    @Nested
    @DisplayName("Encapsulation & Security")
    class EncapsulationTests {

        @Test
        @DisplayName("getCards() should return a defensive copy to prevent external mutation")
        void getCards_ReturnsDefensiveCopy() {
            List<Card> externalList = deck.getCards();
            int originalSize = externalList.size();

            // Act: Modify the list retrieved from the getter
            externalList.clear();

            // Assert: Internal state of the deck remains untouched
            assertEquals(originalSize, deck.getCards().size());
            assertTrue(externalList.isEmpty());
        }
    }

    @Nested
    @DisplayName("Shuffling Behavior")
    class ShufflingTests {

        @Test
        @DisplayName("shuffle() should change the order of cards without losing data")
        void shuffle_RandomizesDeckOrder() {
            List<Card> initialOrder = deck.getCards();

            deck.shuffle();
            List<Card> shuffledOrder = deck.getCards();

            assertEquals(52, shuffledOrder.size());
            assertTrue(shuffledOrder.containsAll(initialOrder));
            assertNotEquals(initialOrder, shuffledOrder); // Probability of 52! matching is effectively zero
        }
    }
}