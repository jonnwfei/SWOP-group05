package base.domain.player;

import base.domain.bid.Bid;
import base.domain.bid.SoloBid;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.strategy.HumanStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Player Entity Rules & State")
class PlayerTest {

    @Mock
    private HumanStrategy mockStrategy;

    @Mock
    private SoloBid mockBid;

    private Player player;
    private PlayerId defaultPlayerId;

    @BeforeEach
    void setUp() {
        defaultPlayerId = new PlayerId();
        player = new Player(mockStrategy, "Alice", defaultPlayerId);
    }

    @Nested
    @DisplayName("Constructor & Initialization")
    class ConstructorTests {

        @Test
        @DisplayName("Should initialize player using the 3-argument constructor")
        void createsPlayerSuccessfully() {
            assertNotNull(player.getId());
            assertNotNull(player.getId().id());
            assertEquals(defaultPlayerId, player.getId());
            assertEquals("Alice", player.getName());
            assertEquals(0, player.getScore());
            assertTrue(player.getHand().isEmpty());
            assertEquals(mockStrategy, player.getDecisionStrategy());
        }

        @Test
        @DisplayName("Should initialize player correctly using the 2-argument constructor")
        void createsPlayerWithTwoArgs() {
            Player twoArgPlayer = new Player(mockStrategy, "Bob");

            assertEquals("Bob", twoArgPlayer.getName());
            assertNotNull(twoArgPlayer.getId(), "A unique ID should be auto-generated.");
            assertEquals(mockStrategy, twoArgPlayer.getDecisionStrategy());
            assertEquals(0, twoArgPlayer.getScore());
        }

        @Test
        @DisplayName("Should reject null strategy, name, or explicit ID in 3-arg constructor")
        void throwsOnNullInputsThreeArg() {
            PlayerId validId = new PlayerId();

            assertThrows(IllegalArgumentException.class, () -> new Player(null, "Alice", validId));
            assertThrows(IllegalArgumentException.class, () -> new Player(mockStrategy, null, validId));
            assertThrows(IllegalArgumentException.class, () -> new Player(mockStrategy, "Alice", null));
        }
    }

    @Nested
    @DisplayName("Hand Management & Querying")
    class HandManagementTests {

        @Test
        @DisplayName("setHand() should throw exception on null input")
        void setHandThrowsOnNull() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> player.setHand(null));
            assertTrue(ex.getMessage().contains("can't be null"));
        }

        @Test
        @DisplayName("setHand() should clear old cards, apply new list, and sort by Suit then Rank (high-to-low)")
        void setsAndSortsHand() {
            // Unordered cards
            Card lowHeart = new Card(Suit.HEARTS, Rank.TWO);
            Card highHeart = new Card(Suit.HEARTS, Rank.ACE);
            Card club = new Card(Suit.CLUBS, Rank.KING);
            Card spade = new Card(Suit.SPADES, Rank.TEN);

            player.setHand(List.of(spade, lowHeart, club, highHeart));
            List<Card> hand = player.getHand();

            // Expected Whist Sorting: Clubs -> Hearts (Ace then 2) -> Spades
            List<Card> expectedOrder = List.of(club, highHeart, lowHeart, spade);
            assertEquals(expectedOrder, hand);
        }

        @Test
        @DisplayName("flushHand() should completely empty the player's hand")
        void flushHandEmptiesHand() {
            player.setHand(List.of(new Card(Suit.HEARTS, Rank.TEN)));
            assertFalse(player.getHand().isEmpty());

            player.flushHand();
            assertTrue(player.getHand().isEmpty());
        }

        @Test
        @DisplayName("getHand() should return a defensive copy to prevent tampering")
        void getHandIsDefensive() {
            player.setHand(List.of(new Card(Suit.HEARTS, Rank.TEN)));

            List<Card> externalHand = player.getHand();
            externalHand.clear(); // Attempt to tamper

            assertFalse(player.getHand().isEmpty(), "Internal hand should remain intact.");
        }

        @Test
        @DisplayName("removeCard() should successfully remove a valid card")
        void removesValidCard() {
            Card heartTen = new Card(Suit.HEARTS, Rank.TEN);
            player.setHand(List.of(heartTen));

            player.removeCard(heartTen);
            assertTrue(player.getHand().isEmpty());
        }

        @Test
        @DisplayName("removeCard() should throw an exception if card is missing or null")
        void removeCardValidation() {
            Card heartTen = new Card(Suit.HEARTS, Rank.TEN);
            player.setHand(List.of(new Card(Suit.SPADES, Rank.TWO)));

            assertThrows(IllegalArgumentException.class, () -> player.removeCard(null));

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> player.removeCard(heartTen));
            assertTrue(exception.getMessage().contains("not in player hand"));
        }

        @Test
        @DisplayName("hasCard() correctly identifies if a card is held")
        void hasCardLogic() {
            Card heldCard = new Card(Suit.HEARTS, Rank.TEN);
            Card missingCard = new Card(Suit.SPADES, Rank.ACE);

            player.setHand(List.of(heldCard));

            assertTrue(player.hasCard(heldCard));
            assertFalse(player.hasCard(missingCard));
        }

        @Test
        @DisplayName("hasCard() throws exception on null input")
        void hasCardThrowsOnNull() {
            assertThrows(IllegalArgumentException.class, () -> player.hasCard(null));
        }
    }

    @Nested
    @DisplayName("Strategy Delegation")
    class StrategyDelegationTests {

        @Test
        @DisplayName("chooseCard() should delegate decision to strategy")
        void delegatesChooseCard() {
            Card expected = new Card(Suit.HEARTS, Rank.TEN);
            when(mockStrategy.chooseCardToPlay(, anyList(), eq(Suit.HEARTS))).thenReturn(expected);

            Card result = player.chooseCard(Suit.HEARTS);

            assertEquals(expected, result);
            verify(mockStrategy).chooseCardToPlay(, anyList(), eq(Suit.HEARTS));
        }

        @Test
        @DisplayName("chooseBid() should delegate decision to strategy using player's ID")
        void delegatesChooseBid() {
            when(mockStrategy.determineBid(eq(player.getId()), anyList())).thenReturn(mockBid);

            Bid result = player.chooseBid();

            assertEquals(mockBid, result);
            verify(mockStrategy).determineBid(eq(player.getId()), anyList());
        }
    }

    @Nested
    @DisplayName("Score Tracking")
    class ScoreTests {

        @Test
        @DisplayName("updateScore() should accumulate both positive and negative deltas")
        void updatesScore() {
            assertEquals(0, player.getScore()); // Initial state

            player.updateScore(15);
            assertEquals(15, player.getScore());

            player.updateScore(-5);
            assertEquals(10, player.getScore());
        }
    }
}