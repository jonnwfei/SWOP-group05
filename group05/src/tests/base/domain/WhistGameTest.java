package base.domain;

import base.domain.player.HumanStrategy;
import base.domain.player.Player;
import base.domain.round.Round;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WhistGameTest {
    WhistGame game;
    Round currentRound;
    Player p1, p2, p3, p4;
    List<Player> players;


    @BeforeEach
    void setUp() {
        game = new WhistGame();
        p1 = new Player(new HumanStrategy(), "Stan");
        p2 = new Player(new HumanStrategy(), "Seppe");
        p3 = new Player(new HumanStrategy(), "Tommy");
        p4 = new Player(new HumanStrategy(), "John");

        players = List.of(p1, p2, p3, p4);

        currentRound = new Round(players, p1, 1);
    }

    @Test
    void addAndGetPlayers() {
        Player p1 = new Player(new HumanStrategy(), "Stan");
        game.addPlayer(p1);

        List<Player> players = game.getPlayers();
        assertEquals(1, players.size());
        assertEquals("Stan", players.getFirst().getName());
        // Check of het een shallow copy is
        assertNotSame(players, game.getPlayers());
    }

    @Test
    void addAndGetCurrentRound() {
        assertNull(game.getCurrentRound()); // First No round added yet

        game.addRound(currentRound);
        assertEquals(currentRound, game.getCurrentRound());
    }

    @Test
    void getCurrentPlayer() {
        assertNull(game.getCurrentPlayer(), "CurrentPlayer moet initieel null zijn");
    }

    @Test
    void stateTransitionFlow() {
        // Test of executeState en nextState de interne state aanpassen
        // Gezien MenuState de start-state is:
        assertNotNull(game.executeState("1"), "Zou een GameEvent moeten teruggeven");

        // TODO: Test de daadwerkelijke overgang naar de volgende state
        // game.nextState();
        // Hiervoor moet de 'state' variabele in WhistGame toegankelijk zijn (bijv. via een getter)
        // om te valideren of de klasse is veranderd.
    }
}