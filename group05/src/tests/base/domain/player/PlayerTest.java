package base.domain.player;

import base.domain.bid.Bid;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.strategy.Strategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("Player Entity Rules & State")
class PlayerTest {

    @Mock
    private Strategy mockStrategy;

    @Mock
    private Bid mockBid;

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player(mockStrategy, "Alice");
    }

    @Nested
    @DisplayName("Constructor Constraints")
    class ConstructorTests {

        @Test
        @DisplayName("Should create player successfully with valid inputs")
        void createsPlayerSuccessfully() {
            assertNotNull(player.getId(), "PlayerId should be automatically generated.");
            assertEquals("Alice", player.getName());
            assertEquals(0, player.getScore(), "Initial score should be 0.");
            assertTrue(player.getHand().isEmpty(), "Initial hand should be empty.");
            assertEquals(mockStrategy, player.getDecisionStrategy());
        }

        @Test
        @DisplayName("Should throw exception if strategy or name is null")
        void throwsOnNullInputs() {
            assertThrows(IllegalArgumentException.class, () -> new Player(null, "Alice"));
            assertThrows(IllegalArgumentException.class, () -> new Player(mockStrategy, null));
        }
    }

    @Nested
    @DisplayName("Hand Management (Cards & Suits)")
    class HandManagementTests {

        @Test
        @DisplayName("setHand() should clear old cards, add new ones, and sort them")
        void setsAndSortsHand() {
            Card lowHeart = new Card(Suit.HEARTS, Rank.TWO);
            Card highHeart = new Card(Suit.HEARTS, Rank.ACE);
            Card spade = new Card(Suit.SPADES, Rank.TEN);
            Card club = new Card(Suit.CLUBS, Rank.KING);

            // Adding them in random order
            List<Card> unsortedHand = List.of(spade, lowHeart, club, highHeart);

            player.setHand(unsortedHand);
            List<Card> sortedHand = player.getHand();

            assertEquals(4, sortedHand.size());

            // Verifying the custom sort logic: Suit natural order, then Rank High-to-Low
            assertTrue(sortedHand.indexOf(club) < sortedHand.indexOf(highHeart)); // Clubs before Hearts
            assertTrue(sortedHand.indexOf(highHeart) < sortedHand.indexOf(lowHeart)); // Ace before Two
            assertTrue(sortedHand.indexOf(lowHeart) < sortedHand.indexOf(spade)); // Hearts before Spades
        }

        @Test
        @DisplayName("setHand() should throw exception on null")
        void setHandThrowsOnNull() {
            assertThrows(IllegalArgumentException.class, () -> player.setHand(null));
        }

        @Test
        @DisplayName("getHand() should return a defensive copy")
        void getHandIsDefensive() {
            player.setHand(List.of(new Card(Suit.HEARTS, Rank.TEN)));

            List<Card> externalHand = player.getHand();
            externalHand.clear(); // Maliciously altering the returned list

            assertFalse(player.getHand().isEmpty(), "Internal hand should not be affected by external modifications.");
        }

        @Test
        @DisplayName("flushHand() should empty the current hand")
        void flushesHand() {
            player.setHand(List.of(new Card(Suit.HEARTS, Rank.TEN)));
            player.flushHand();
            assertTrue(player.getHand().isEmpty());
        }

        @Test
        @DisplayName("hasSuit() correctly identifies presence or absence of a suit")
        void checksForSuit() {
            player.setHand(List.of(new Card(Suit.HEARTS, Rank.TEN)));

            assertTrue(player.hasSuit(Suit.HEARTS));
            assertFalse(player.hasSuit(Suit.SPADES));
            assertThrows(IllegalArgumentException.class, () -> player.hasSuit(null));
        }

        @Test
        @DisplayName("hasCard() correctly identifies presence or absence of a specific card")
        void checksForSpecificCard() {
            Card heartTen = new Card(Suit.HEARTS, Rank.TEN);
            player.setHand(List.of(heartTen));

            assertTrue(player.hasCard(heartTen));
            assertFalse(player.hasCard(new Card(Suit.SPADES, Rank.TEN)));
            assertThrows(IllegalArgumentException.class, () -> player.hasCard(null));
        }

        @Test
        @DisplayName("removeCard() successfully removes card or throws if missing")
        void removesCard() {
            Card heartTen = new Card(Suit.HEARTS, Rank.TEN);
            player.setHand(List.of(heartTen));

            player.removeCard(heartTen);
            assertTrue(player.getHand().isEmpty());

            assertThrows(IllegalArgumentException.class, () -> player.removeCard(heartTen), "Should throw if card is no longer in hand.");
            assertThrows(IllegalArgumentException.class, () -> player.removeCard(null));
        }
    }

    @Nested
    @DisplayName("Strategy Delegation")
    class StrategyDelegationTests {

        @Test
        @DisplayName("chooseCard() delegates correctly to the Strategy")
        void delegatesChooseCard() {
            Card expectedCard = new Card(Suit.HEARTS, Rank.TEN);
            player.setHand(List.of(expectedCard));

            // MOCKITO MAGIC: "When the strategy is asked to choose a card, force it to return this specific card."
            when(mockStrategy.chooseCardToPlay(anyList(), eq(Suit.HEARTS))).thenReturn(expectedCard);

            Card playedCard = player.chooseCard(Suit.HEARTS);

            assertEquals(expectedCard, playedCard);
            verify(mockStrategy, times(1)).chooseCardToPlay(anyList(), eq(Suit.HEARTS));
        }

        @Test
        @DisplayName("chooseBid() delegates correctly to the Strategy")
        void delegatesChooseBid() {
            // MOCKITO MAGIC: Force the strategy to return our mock bid.
            when(mockStrategy.determineBid(eq(player.getId()), anyList())).thenReturn(mockBid);

            Bid chosenBid = player.chooseBid();

            assertEquals(mockBid, chosenBid);
            verify(mockStrategy, times(1)).determineBid(eq(player.getId()), anyList());
        }

        @Test
        @DisplayName("getRequiresConfirmation() delegates to Strategy")
        void delegatesRequiresConfirmation() {
            when(mockStrategy.requiresConfirmation()).thenReturn(true);
            assertTrue(player.getRequiresConfirmation());

            when(mockStrategy.requiresConfirmation()).thenReturn(false);
            assertFalse(player.getRequiresConfirmation());
        }
    }

    @Nested
    @DisplayName("Score Management")
    class ScoreTests {

        @Test
        @DisplayName("updateScore() accumulates points correctly")
        void updatesScore() {
            player.updateScore(15);
            assertEquals(15, player.getScore());

            player.updateScore(-5);
            assertEquals(10, player.getScore());
        }
    }
}