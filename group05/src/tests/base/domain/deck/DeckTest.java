package base.domain.deck;

import base.domain.card.Card;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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

            // AssertJ chains make collection testing very readable
            assertThat(cards)
                    .hasSize(52)
                    .doesNotContainNull()
                    .containsOnlyOnceElementsOf(cards);
        }
    }

    @Nested
    @DisplayName("Dealing Logic")
    class DealingTests {

        @Test
        @DisplayName("Should distribute 52 cards into four hands of 13 cards each")
        void deal_DistributesCardsCorrectlyToFourPlayers() {
            List<List<Card>> hands = deck.deal();

            assertThat(hands).hasSize(4);

            // Flatten all hands to check the total pool of dealt cards
            List<Card> allDealtCards = hands.stream()
                    .flatMap(List::stream)
                    .toList();

            assertThat(allDealtCards).hasSize(52).doesNotHaveDuplicates();

            for (List<Card> hand : hands) {
                assertThat(hand).hasSize(13);
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
            assertThat(deck.getCards()).hasSize(originalSize);
            assertThat(externalList).isEmpty();
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

            assertThat(shuffledOrder)
                    .hasSize(52)
                    .containsExactlyInAnyOrderElementsOf(initialOrder)
                    .isNotEqualTo(initialOrder); // Probability of 52! matching is effectively zero
        }
    }
}