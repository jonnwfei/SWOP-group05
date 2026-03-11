package base.domain.player;

import base.domain.bid.Bid;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    private Player player;
    private Strategy mockStrategy;

    @BeforeEach
    void setUp() {
        // Using HighBotStrategy as a concrete implementation for the test
        mockStrategy = new HighBotStrategy();
        player = new Player(mockStrategy, "Jane Doe");
    }

    // -------- CONSTRUCTOR TESTS --------

    @Test
    void constructor_NullValues_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Player(null, "Jane Doe"));
        assertThrows(IllegalArgumentException.class, () -> new Player(mockStrategy, null));
    }

    @Test
    void constructor_InitializesProperly() {
        assertEquals("Jane Doe", player.getName());
        assertEquals(0, player.getScore());
        assertTrue(player.getHand().isEmpty());
    }

    // -------- HAND MANAGEMENT TESTS --------

    @Test
    void setHand_SortsAndStoresCards() {
        Card clubsTwo = new Card(Suit.CLUBS, Rank.TWO);
        Card heartsAce = new Card(Suit.HEARTS, Rank.ACE);
        Card heartsTwo = new Card(Suit.HEARTS, Rank.TWO);
        Card clubsAce = new Card(Suit.CLUBS, Rank.ACE);

        List<Card> unsortedHand = List.of(clubsTwo, heartsAce, heartsTwo, clubsAce);
        player.setHand(unsortedHand);

        List<Card> sortedHand = player.getHand();

        // Ensure size is correct
        assertEquals(4, sortedHand.size());

        // Validate Sorting Rules: Suits grouped together, then high-to-low Rank
        // Note: The exact order of suits depends on how the Suit enum is declared.
        // Assuming CLUBS comes before HEARTS in the enum:
        assertEquals(clubsAce, sortedHand.get(0));
        assertEquals(clubsTwo, sortedHand.get(1));
        assertEquals(heartsAce, sortedHand.get(2));
        assertEquals(heartsTwo, sortedHand.get(3));
    }

    @Test
    void setHand_NullThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> player.setHand(null));
    }

    @Test
    void flushHand_ClearsHand() {
        player.setHand(List.of(new Card(Suit.HEARTS, Rank.ACE)));
        assertFalse(player.getHand().isEmpty());

        player.flushHand();
        assertTrue(player.getHand().isEmpty());
    }

    @Test
    void hasSuit_ReturnsCorrectly() {
        player.setHand(List.of(new Card(Suit.HEARTS, Rank.ACE)));

        assertTrue(player.hasSuit(Suit.HEARTS));
        assertFalse(player.hasSuit(Suit.SPADES));
    }

    @Test
    void hasSuit_NullThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> player.hasSuit(null));
    }

    @Test
    void removeCard_RemovesTargetCard() {
        Card targetCard = new Card(Suit.DIAMONDS, Rank.KING);
        player.setHand(new ArrayList<>(List.of(targetCard, new Card(Suit.HEARTS, Rank.TWO))));

        player.removeCard(targetCard);

        assertEquals(1, player.getHand().size());
        assertFalse(player.getHand().contains(targetCard));
    }

    @Test
    void removeCard_InvalidOrNullThrowsException() {
        Card missingCard = new Card(Suit.SPADES, Rank.ACE);
        player.setHand(List.of(new Card(Suit.HEARTS, Rank.TWO)));

        assertThrows(IllegalArgumentException.class, () -> player.removeCard(null));
        assertThrows(IllegalArgumentException.class, () -> player.removeCard(missingCard));
    }

    @Test
    void getHand_ReturnsDefensiveCopy() {
        player.setHand(List.of(new Card(Suit.HEARTS, Rank.TWO)));

        List<Card> externalHand = player.getHand();
        externalHand.clear(); // Attempt to sabotage the player's hand

        assertFalse(player.getHand().isEmpty(), "Internal hand should remain unaffected.");
    }

    // -------- SCORE TESTS --------

    @Test
    void updateScore_AddsAndSubtracts() {
        player.updateScore(10);
        assertEquals(10, player.getScore());

        player.updateScore(-5);
        assertEquals(5, player.getScore());
    }

    // -------- STRATEGY DELEGATION TESTS --------

    @Test
    void chooseBid_DelegatesToStrategy() {
        Bid bid = player.chooseBid();
        assertNotNull(bid);
        assertEquals(player, bid.getPlayer(), "The bid should be linked to the calling player.");
    }

    @Test
    void chooseCard_DelegatesToStrategy() {
        player.setHand(List.of(new Card(Suit.HEARTS, Rank.ACE)));

        // With HighBotStrategy, it should pick the highest card (the only card in this case)
        Card chosen = player.chooseCard(Suit.HEARTS);

        assertNotNull(chosen);
        assertEquals(Suit.HEARTS, chosen.suit());
    }

    @Test
    void getRequiresConfirmation_ReflectsStrategy() {
        // HighBotStrategy returns false
        assertFalse(player.getRequiresConfirmation());
    }

}