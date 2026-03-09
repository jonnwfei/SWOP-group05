package base.domain.player;

import base.domain.bid.Bid;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    private Player player;
    private Strategy mockStrategy;

    @BeforeEach
    void setUp() {
        // We gebruiken een echte HighBotStrategy als simpele implementatie voor de test
        mockStrategy = new HighBotStrategy();
        player = new Player(mockStrategy, "Jane Doe");
    }

    @Test
    void constructor_NullValues_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Player(null, "Jane Doe"));
        assertThrows(IllegalArgumentException.class, () -> new Player(mockStrategy, null));
    }





    @Test
    void updateScore_AddsAndSubtracts() {
        player.updateScore(10);
        assertEquals(10, player.getScore());
        player.updateScore(-5);
        assertEquals(5, player.getScore());
    }



    @Test
    void chooseBid_DelegatesToStrategy() {
        Bid bid = player.chooseBid();
        assertNotNull(bid);
        assertEquals(player, bid.getPlayer(), "De bid moet gekoppeld zijn aan de speler die hem kiest.");
    }

    @Test
    void getName_ReturnsCorrectName() {
        assertEquals("Jane Doe", player.getName());
    }

    @Test
    void getRequiresConfirmation_ReflectsStrategy() {
        // HighBotStrategy geeft false terug
        assertFalse(player.getRequiresConfirmation());
    }
}