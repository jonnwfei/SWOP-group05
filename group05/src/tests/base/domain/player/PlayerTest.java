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
    void addCard_IncreasesHandSize() {
        Card card = new Card(Suit.HEARTS, Rank.ACE);
        player.addCard(card);
        assertEquals(1, player.getHand().size());
        assertTrue(player.getHand().contains(card));
    }

    @Test
    void addCard_FullHand_ThrowsException() {
        // Vul de hand met 13 kaarten
        for (int i = 0; i < 13; i++) {
            player.addCard(new Card(Suit.CLUBS, Rank.TWO));
        }
        // De 14e kaart moet een foutmelding geven
        assertThrows(IllegalStateException.class, () -> player.addCard(new Card(Suit.CLUBS, Rank.THREE)));
    }

    @Test
    void hasSuit_CorrectlyIdentifiesSuit() {
        player.addCard(new Card(Suit.DIAMONDS, Rank.TEN));
        assertTrue(player.hasSuit(Suit.DIAMONDS));
        assertFalse(player.hasSuit(Suit.SPADES));
    }

    @Test
    void flushHand_EmptiesHand() {
        player.addCard(new Card(Suit.HEARTS, Rank.KING));
        player.flushHand();
        assertTrue(player.getHand().isEmpty());
    }

    @Test
    void removeCard_SuccessAndFailure() {
        Card card = new Card(Suit.SPADES, Rank.JACK);
        player.addCard(card);

        // Verwijder bestaande kaart
        player.removeCard(card);
        assertEquals(0, player.getHand().size());

        // Probeer kaart te verwijderen die niet in hand zit
        assertThrows(IllegalArgumentException.class, () -> player.removeCard(card));
    }

    @Test
    void updateScore_AddsAndSubtracts() {
        player.updateScore(10);
        assertEquals(10, player.getScore());
        player.updateScore(-5);
        assertEquals(5, player.getScore());
    }

    @Test
    void getHand_IsDefensiveCopy() {
        player.addCard(new Card(Suit.HEARTS, Rank.TWO));
        List<Card> handCopy = player.getHand();

        // Als we de kopie aanpassen, mag de originele hand niet veranderen
        assertThrows(UnsupportedOperationException.class, () -> handCopy.add(new Card(Suit.CLUBS, Rank.THREE)),
                "De getHand() methode zou idealiter een unmodifiable list of een kopie moeten geven die de originele state beschermt.");

        // Check of het een andere lijst-instantie is
        assertNotSame(player.getHand(), player.getHand());
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