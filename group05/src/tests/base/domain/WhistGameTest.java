package base.domain;

import base.domain.player.HumanStrategy;
import base.domain.player.Player;
import base.domain.round.Round;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WhistGameTest {

    private WhistGame game;

    @BeforeEach
    void setUp() {
        game = new WhistGame();
    }

    @Test
    void addAndGetPlayers() {
        Player p1 = new Player(new HumanStrategy(), "Stan");
        game.addPlayer(p1);

        List<Player> players = game.getPlayers();
        assertEquals(1, players.size());
        assertEquals("Stan", players.get(0).getName());
        // Check of het een shallow copy is
        assertNotSame(players, game.getPlayers());
    }

    @Test
    void addAndGetCurrentRound() {
        // TODO: Vervang 'null' door een mock of echt Round object zodra de constructor bekend is
        // Round round = new Round(...);
        // game.addRound(round);
        // assertEquals(round, game.getCurrentRound());

        assertThrows(java.util.NoSuchElementException.class, () -> game.getCurrentRound(),
                "Zou moeten falen als er nog geen rondes zijn toegevoegd");
    }

    @Test
    void getDealerPlayer() {
        // TODO: Er is momenteel geen setter voor dealerPlayer in WhistGame.
        // Schrijf hier een test zodra de logica voor het aanwijzen van de deler bekend is.
        assertNull(game.getDealerPlayer(), "Dealer moet initieel null zijn");
    }

    @Test
    void getCurrentPlayer() {
        // TODO: Er is momenteel geen logica om currentPlayer te veranderen in WhistGame.
        assertNull(game.getCurrentPlayer(), "CurrentPlayer moet initieel null zijn");
    }

    @Test
    void printNames() {
        game.addPlayer(new Player(new HumanStrategy(), "Alice"));
        game.addPlayer(new Player(new HumanStrategy(), "Bob"));

        String expected = "Players in this game:\n- Alice\n- Bob\n";

    }

    @Test
    void printScore() {
        Player p = new Player(new HumanStrategy(), "Alice");
        // TODO: Als Player.setScore bestaat, zet hier een score om de weergave te testen
        game.addPlayer(p);

        String output = game.printScore();
        assertTrue(output.contains("Alice:"));
        assertTrue(output.contains("0 points")); // Uitgaande van beginscore 0
        assertTrue(output.startsWith("======= SCORES ======="));
    }

    @Test
    void stateTransitionFlow() {
        // Test of executeState en nextState de interne state aanpassen
        // Gezien MenuState de start-state is:


        // TODO: Test de daadwerkelijke overgang naar de volgende state
        // game.nextState();
        // Hiervoor moet de 'state' variabele in WhistGame toegankelijk zijn (bijv. via een getter)
        // om te valideren of de klasse is veranderd.
    }
}