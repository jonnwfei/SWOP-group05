package base.domain.trick;

import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.player.HumanStrategy;
import base.domain.player.LowBotStrategy;
import base.domain.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TrickTest {
    private Player p1;
    private Player p2;
    private Player p3;
    private Player p4;
    private Player p5;
    private Trick currentTrick;


    @BeforeEach
    void setUp() {
        p1 = new Player(new HumanStrategy(), "P1");
        p2 = new Player(new LowBotStrategy(), "P2");
        p3 = new Player(new LowBotStrategy(), "P3");
        p4 = new Player(new LowBotStrategy(), "P4");
        p5 = new Player(new LowBotStrategy(), "P5");

        currentTrick = new Trick(p1, Suit.CLUBS);
    }

    @Test
    void getStartingPlayer() {
        assertEquals(p1, currentTrick.getStartingPlayer());
    }

    @Test
    void getWinningPlayer() {
        assertNull(currentTrick.getWinningPlayer());
    }

    @Test
    void constructorThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Trick(null, Suit.CLUBS));


    }

    @Test
    void playCardValidation() {
        assertThrows(IllegalArgumentException.class, () -> currentTrick.playCard(null, new Card(Suit.HEARTS, Rank.ACE)));
        assertThrows(IllegalArgumentException.class, () -> currentTrick.playCard(p1, null));

        p1.setHand(List.of(new Card(Suit.HEARTS, Rank.ACE), new Card(Suit.HEARTS, Rank.KING)));
        currentTrick.playCard(p1, new Card(Suit.HEARTS, Rank.ACE));

        assertFalse(p1.getHand().contains(new Card(Suit.HEARTS, Rank.ACE))); // check if removed or not
        // cant play twice
        assertThrows(IllegalArgumentException.class, () -> currentTrick.playCard(p1, new Card(Suit.HEARTS, Rank.KING)));

        p2.setHand(List.of(new Card(Suit.DIAMONDS, Rank.ACE), new Card(Suit.HEARTS, Rank.QUEEN)));
        assertThrows(IllegalArgumentException.class, () -> currentTrick.playCard(p2, new Card(Suit.DIAMONDS, Rank.ACE))); // You have to follow leadsuit
        currentTrick.playCard(p2, new Card(Suit.HEARTS, Rank.QUEEN));

        // fill trick
        p3.setHand(List.of(new Card(Suit.CLUBS, Rank.ACE), new Card(Suit.CLUBS, Rank.QUEEN)));
        currentTrick.playCard(p3, new Card(Suit.CLUBS, Rank.ACE));

        p4.setHand(List.of(new Card(Suit.SPADES, Rank.ACE), new Card(Suit.SPADES, Rank.QUEEN)));
        currentTrick.playCard(p4, new Card(Suit.SPADES, Rank.ACE));
//        System.out.println(currentTrick.getTurns().size());

        // Trick is full cant play more
        p5.setHand(List.of(new Card(Suit.SPADES, Rank.ACE), new Card(Suit.SPADES, Rank.QUEEN)));
        assertThrows(IllegalArgumentException.class, () -> currentTrick.playCard(p5, new Card(Suit.SPADES, Rank.ACE)));


    }

    @Test
    void getLeadingSuit() {
        assertNull(currentTrick.getLeadingSuit());

        p1.setHand(List.of(new Card(Suit.HEARTS, Rank.ACE), new Card(Suit.HEARTS, Rank.KING)));
        currentTrick.playCard(p1, new Card(Suit.HEARTS, Rank.ACE));
        assertEquals(Suit.HEARTS, currentTrick.getLeadingSuit());
    }

    @Test
    void getTurns() {
        p1.setHand(List.of(new Card(Suit.HEARTS, Rank.ACE), new Card(Suit.HEARTS, Rank.KING)));
        currentTrick.playCard(p1, new Card(Suit.HEARTS, Rank.ACE));

        p2.setHand(List.of(new Card(Suit.DIAMONDS, Rank.ACE), new Card(Suit.HEARTS, Rank.QUEEN)));
        currentTrick.playCard(p2, new Card(Suit.HEARTS, Rank.QUEEN));

        assertEquals(2, currentTrick.getTurns().size());
        assertEquals(new Turn(p1, new Card(Suit.HEARTS, Rank.ACE)), currentTrick.getTurns().getFirst());
        assertEquals(new Turn(p2, new Card(Suit.HEARTS, Rank.QUEEN)), currentTrick.getTurns().get(1));
    }

    @Test
    void isCompleted() {
        assertFalse(currentTrick.isCompleted());
        playTestTrickTrumpWins();
        assertTrue(currentTrick.isCompleted());

    }

    /**
     * Plays a test trick simulating a full round of 4 turns:
     * <ul>
     * <li><b>Turn 1:</b> p1 leads the trick by playing the ACE of HEARTS.</li>
     * <li><b>Turn 2:</b> p2 follows suit by playing the QUEEN of HEARTS.</li>
     * <li><b>Turn 3:</b> p3 cannot follow suit and plays the ACE of CLUBS.</li>
     * <li><b>Turn 4:</b> p4 cannot follow suit and plays the ACE of SPADES.</li>
     * </ul>
     */
    void playTestTrickTrumpWins() {
        p1.setHand(List.of(new Card(Suit.HEARTS, Rank.ACE), new Card(Suit.HEARTS, Rank.KING)));
        currentTrick.playCard(p1, new Card(Suit.HEARTS, Rank.ACE));
        p2.setHand(List.of(new Card(Suit.DIAMONDS, Rank.ACE), new Card(Suit.HEARTS, Rank.QUEEN)));
        currentTrick.playCard(p2, new Card(Suit.HEARTS, Rank.QUEEN));
        p3.setHand(List.of(new Card(Suit.CLUBS, Rank.ACE), new Card(Suit.CLUBS, Rank.QUEEN)));
        currentTrick.playCard(p3, new Card(Suit.CLUBS, Rank.ACE));
        p4.setHand(List.of(new Card(Suit.SPADES, Rank.ACE), new Card(Suit.SPADES, Rank.QUEEN)));
        currentTrick.playCard(p4, new Card(Suit.SPADES, Rank.ACE));
    }

    @Test
    void determineWinner_HigherLeadBeatsLowerLead() {
        // P1 leads, but P2 follows suit with a higher card. No trumps played.
        p1.setHand(List.of(new Card(Suit.HEARTS, Rank.TEN)));
        currentTrick.playCard(p1, new Card(Suit.HEARTS, Rank.TEN));

        p2.setHand(List.of(new Card(Suit.HEARTS, Rank.ACE))); // Higher lead suit
        currentTrick.playCard(p2, new Card(Suit.HEARTS, Rank.ACE));

        p3.setHand(List.of(new Card(Suit.DIAMONDS, Rank.ACE))); // Off suit
        currentTrick.playCard(p3, new Card(Suit.DIAMONDS, Rank.ACE));

        p4.setHand(List.of(new Card(Suit.SPADES, Rank.ACE))); // Off suit
        currentTrick.playCard(p4, new Card(Suit.SPADES, Rank.ACE));

        assertEquals(p2, currentTrick.getWinningPlayer());
    }

    @Test
    void determineWinner_HigherTrumpBeatsLowerTrump() {
        // P1 leads, P2 plays low Trump, P3 plays high Trump, P4 plays off-suit
        p1.setHand(List.of(new Card(Suit.HEARTS, Rank.ACE)));
        currentTrick.playCard(p1, new Card(Suit.HEARTS, Rank.ACE));

        p2.setHand(List.of(new Card(Suit.CLUBS, Rank.TWO))); // LOW TRUMP
        currentTrick.playCard(p2, new Card(Suit.CLUBS, Rank.TWO));

        p3.setHand(List.of(new Card(Suit.CLUBS, Rank.ACE))); // HIGH TRUMP
        currentTrick.playCard(p3, new Card(Suit.CLUBS, Rank.ACE));

        p4.setHand(List.of(new Card(Suit.CLUBS, Rank.TEN))); // LOWER TRUMP (Checks false branch)
        currentTrick.playCard(p4, new Card(Suit.CLUBS, Rank.TEN));

        assertEquals(p3, currentTrick.getWinningPlayer());
    }

    @Test
    void determineWinner_NoTrumpGame() {
        // Setup trick with NULL trump suit (e.g. Miserie)
        Trick miserieTrick = new Trick(p1, null);

        p1.setHand(List.of(new Card(Suit.HEARTS, Rank.TEN)));
        miserieTrick.playCard(p1, new Card(Suit.HEARTS, Rank.TEN));

        p2.setHand(List.of(new Card(Suit.HEARTS, Rank.ACE))); // Higher lead suit wins
        miserieTrick.playCard(p2, new Card(Suit.HEARTS, Rank.ACE));

        p3.setHand(List.of(new Card(Suit.CLUBS, Rank.ACE))); // Normally trump, but null here so it loses
        miserieTrick.playCard(p3, new Card(Suit.CLUBS, Rank.ACE));

        p4.setHand(List.of(new Card(Suit.SPADES, Rank.ACE)));
        miserieTrick.playCard(p4, new Card(Suit.SPADES, Rank.ACE));

        assertEquals(p2, miserieTrick.getWinningPlayer());
    }
}