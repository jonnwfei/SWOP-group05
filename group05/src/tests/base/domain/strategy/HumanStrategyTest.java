package base.domain.strategy;

import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HumanStrategyTest {

    private HumanStrategy humanStrategy;

    @BeforeEach
    void setUp() {
        humanStrategy = new HumanStrategy();
    }

    @Test
    void determineBid_ReturnsNull() {
        // For human players, bidding is handled via the UI/State machine rather than algorithmically.
        // Thus, the domain strategy currently acts as a stub and safely returns null.
        Player dummyPlayer = new Player(humanStrategy, "Test Human");

        assertNull(humanStrategy.determineBid(dummyPlayer),
                "Human strategy should return null as UI handles the actual bid creation.");
    }

    @Test
    void chooseCardToPlay_ReturnsNull() {
        // Similarly, card selection for humans relies on external I/O.
        // This method serves as a placeholder to satisfy the Strategy interface.
        List<Card> dummyHand = List.of(new Card(Suit.HEARTS, Rank.ACE));
        Suit dummyLead = Suit.HEARTS;

        assertNull(humanStrategy.chooseCardToPlay(dummyHand, dummyLead),
                "Human strategy should return null as UI handles the actual card selection.");
    }

    @Test
    void requiresConfirmation_ReturnsTrue() {
        // A human player explicitly requires UI confirmation (e.g., pressing ENTER)
        // to proceed to the next turn or round, unlike automated bots.
        assertTrue(humanStrategy.requiresConfirmation(),
                "Human strategy must always flag that it requires confirmation.");
    }
}