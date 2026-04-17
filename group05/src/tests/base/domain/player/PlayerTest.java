package base.domain.player;

import base.domain.bid.Bid;
import base.domain.bid.SoloBid;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.strategy.HumanStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("Player Entity Rules & State")
class PlayerTest {

    @Mock
    private HumanStrategy mockStrategy;

    @Mock
    private SoloBid mockBid;

    private AutoCloseable mocks;
    private Player player;
    private PlayerId defaultPlayerId;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        defaultPlayerId = new PlayerId();

        // Using the updated constructor signature: (Strategy, String, PlayerId)
        player = new Player(mockStrategy, "Alice", defaultPlayerId);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @Nested
    @DisplayName("Constructor & Initialization")
    class ConstructorTests {

        @Test
        @DisplayName("Should initialize player with an explicit ID and empty state")
        void createsPlayerSuccessfully() {
            assertNotNull(player.getId());
            assertNotNull(player.getId().id()); // Ensure the internal UUID exists
            assertEquals(defaultPlayerId, player.getId());
            assertEquals("Alice", player.getName());
            assertEquals(0, player.getScore());
            assertTrue(player.getHand().isEmpty());
            assertEquals(mockStrategy, player.getDecisionStrategy());
        }

        @Test
        @DisplayName("Should reject null strategy, name, or explicit ID")
        void throwsOnNullInputs() {
            PlayerId validId = new PlayerId();

            // Testing the single primary constructor constraints
            assertThrows(IllegalArgumentException.class, () -> new Player(null, "Alice", validId));
            assertThrows(IllegalArgumentException.class, () -> new Player(mockStrategy, null, validId));
            assertThrows(IllegalArgumentException.class, () -> new Player(mockStrategy, "Alice", null));
        }
    }

    @Nested
    @DisplayName("Hand Management")
    class HandManagementTests {

        @Test
        @DisplayName("setHand() should clear old cards and apply new sorted list")
        void setsAndSortsHand() {
            Card lowHeart = new Card(Suit.HEARTS, Rank.TWO);
            Card highHeart = new Card(Suit.HEARTS, Rank.ACE);
            Card club = new Card(Suit.CLUBS, Rank.KING);
            Card spade = new Card(Suit.SPADES, Rank.TEN);

            player.setHand(List.of(spade, lowHeart, club, highHeart));
            List<Card> hand = player.getHand();

            // Verifying Whist Sorting: Clubs -> Hearts (Ace then 2) -> Spades
            List<Card> expectedOrder = List.of(club, highHeart, lowHeart, spade);
            assertEquals(expectedOrder, hand);
        }

        @Test
        @DisplayName("getHand() should return a defensive copy to prevent tampering")
        void getHandIsDefensive() {
            player.setHand(List.of(new Card(Suit.HEARTS, Rank.TEN)));

            List<Card> externalHand = player.getHand();
            externalHand.clear();

            assertFalse(player.getHand().isEmpty());
        }

        @Test
        @DisplayName("removeCard() should update hand or throw if card is missing")
        void removesCard() {
            Card heartTen = new Card(Suit.HEARTS, Rank.TEN);
            player.setHand(List.of(heartTen));

            player.removeCard(heartTen);
            assertTrue(player.getHand().isEmpty());

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> player.removeCard(heartTen));
            assertTrue(exception.getMessage().contains("not in player hand"));
        }
    }

    @Nested
    @DisplayName("Strategy Delegation")
    class StrategyDelegationTests {

        @Test
        @DisplayName("chooseCard() should delegate decision to strategy")
        void delegatesChooseCard() {
            Card expected = new Card(Suit.HEARTS, Rank.TEN);
            when(mockStrategy.chooseCardToPlay(anyList(), eq(Suit.HEARTS))).thenReturn(expected);

            Card result = player.chooseCard(Suit.HEARTS);

            assertEquals(expected, result);
            verify(mockStrategy).chooseCardToPlay(anyList(), eq(Suit.HEARTS));
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
            player.updateScore(15);
            player.updateScore(-5);

            assertEquals(10, player.getScore());
        }
    }
}