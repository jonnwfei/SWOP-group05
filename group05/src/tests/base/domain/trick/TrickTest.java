package base.domain.trick;

import base.domain.card.Suit;
import base.domain.player.HumanStrategy;
import base.domain.player.LowBotStrategy;
import base.domain.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TrickTest {
    private Player p1;
    private Player p2;
    private Suit trumpSuit;
    private Trick currentTrick;


    @BeforeEach
    void setUp() {
        p1 = new Player(new HumanStrategy(),"p1");
        p2 = new Player(new LowBotStrategy(),"lowBot");

        trumpSuit = Suit.CLUBS;

        currentTrick = new Trick(p1, trumpSuit);


    }

    @Test
    void getStartingPlayer() {
        Player p1Test = currentTrick.getStartingPlayer();
        assertEquals(p1, p1Test);
    }

    @Test
    void getWinningPlayer() {
    }

    @Test
    void playCard() {
    }
}