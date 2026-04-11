package base.domain.trick;

import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.player.HumanStrategy;
import base.domain.player.Player;
import base.domain.turn.PlayTurn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class playTurnTest {
    PlayTurn playTurn;
    Player p1;
    Card playedCard;

    @BeforeEach
    void setUp() {
        p1 = new Player(new HumanStrategy(), "P1");
        playedCard = new Card(Suit.HEARTS, Rank.ACE);
        playTurn = new PlayTurn(p1, playedCard);
    }

    @Test
    void testToString() {
        assertEquals("P1 played " + playedCard.toString(), playTurn.toString());
    }

    @Test
    void constructorThrowsOnNull() {
        assertThrows(IllegalArgumentException.class, () -> new PlayTurn(null, playedCard));
        assertThrows(IllegalArgumentException.class, () -> new PlayTurn(p1, null));
    }
}