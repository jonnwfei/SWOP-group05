package usecases;

import base.domain.WhistGame;
import base.domain.player.HumanStrategy;
import base.domain.player.Player;
import base.domain.states.CountState;
import base.domain.states.MenuState;
import base.domain.states.State;
import cli.elements.QuestionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StartNewCount {

    private WhistGame game;
    private State currentState;

    @BeforeEach
    void setUp() {
        game = new WhistGame();
        currentState = new MenuState(game);
    }

    @Test
    void testFullStartNewCountFlow() {
        // --- STAP 1 & 2: Menu & Namen registreren (Scenario 1 & 2) ---
        currentState.executeState(""); // Trigger welkom

        // Kies (2) Count the scores
        currentState.executeState("2");

        // Registreer 4 spelers
        currentState.executeState("John Doe 1");
        currentState.executeState("John Doe 2");
        currentState.executeState("John Doe 3");
        currentState.executeState("John Doe 4");

        // Controleer of spelers zijn toegevoegd en we naar CountState gaan
        assertEquals(4, game.getPlayers().size());
        currentState = currentState.nextState();
        assertTrue(currentState instanceof CountState, "Moet nu in CountState zijn");

        // FIX: Trigger de START fase van CountState
        currentState.executeState("");
        // Kies Bid: (3) Abondance 9
        currentState.executeState("3");

        // Kies Trump: (2) Clubs
        currentState.executeState("2");

        // --- STAP 5: Welke speler speelt het bid? (Scenario 5) ---
        // Player 1 (John Doe 1) speelt solo
        currentState.executeState("2");

        // --- STAP 6 & 7: Resultaat & Berekening (Scenario 6 & 7) ---
        // Scenario 6b: Hoeveel tricks gewonnen? john Doe 1 wint er 10 (Abondance 9 gehaald)
        currentState.executeState("10");
        assertTrue( game.getPlayers().get(1).getScore() > 0, "De bieder moet punten hebben gewonnen");
        assertTrue( game.getPlayers().get(2).getScore() < 0, "Tegenstander moet punten hebben verloren");

    }

    @Test
    void testMiserieCountFlow() {
        // Setup spelers
        game.addPlayer(new Player(new HumanStrategy(), "P1")); // Index 0
        game.addPlayer(new Player(new HumanStrategy(), "P2")); // Index 1
        game.addPlayer(new Player(new HumanStrategy(), "P3")); // Index 2
        game.addPlayer(new Player(new HumanStrategy(), "P4")); // Index 3

        currentState = new CountState(game);

        // 1. Trigger de START fase (fase gaat naar SELECT_BID)
        currentState.executeState("");

        // 2. Selecteer Miserie (fase gaat naar SELECT_TRUMP)
        currentState.executeState("7");

        // 3. Selecteer Trump (fase gaat naar SELECT_PLAYERS)
        currentState.executeState("1");

        // 4. Selecteer spelers P1 en P2 (gebruik "1, 2" want 1-1=0 en 2-1=1)
        currentState.executeState("1, 2");

        // 5. Wie heeft gewonnen? Enkel P1 (gebruik "1" want 1-1=0)
        // Nu gaat de fase naar CALCULATE en worden scores bijgewerkt
        currentState.executeState("1");

        // 6. Assertions op de juiste indices (0 en 1)
        assertTrue(game.getPlayers().get(0).getScore() > 0, "P1 (index 0) moet positieve score hebben");
        assertTrue(game.getPlayers().get(1).getScore() < 0, "P2 (index 1) moet negatieve score hebben");

    }
    @Test
    void testSoloBidSuccessAndReturnToMenu() {
        game.addPlayer(new Player(new HumanStrategy(), "P1"));
        game.addPlayer(new Player(new HumanStrategy(), "P2"));
        game.addPlayer(new Player(new HumanStrategy(), "P3"));
        game.addPlayer(new Player(new HumanStrategy(), "P4"));

        currentState = new CountState(game);
        currentState.executeState("");
        currentState.executeState("9"); // Solo Normal
        currentState.executeState("3"); // Diamonds
        currentState.executeState("1"); // P1 speelt
        currentState.executeState("13"); // 6 slagen (Solo gehaald)

        assertTrue(game.getPlayers().get(0).getScore() > 0);
        assertEquals(1, game.getRounds().size());

        currentState.executeState("2"); // Terug naar menu
        currentState = currentState.nextState();
        assertTrue(currentState instanceof MenuState);
    }
    @Test
    void testProposalWithPartnerFailure() {
        game.addPlayer(new Player(new HumanStrategy(), "P1"));
        game.addPlayer(new Player(new HumanStrategy(), "P2"));
        game.addPlayer(new Player(new HumanStrategy(), "P3"));
        game.addPlayer(new Player(new HumanStrategy(), "P4"));

        currentState = new CountState(game);
        currentState.executeState("");
        currentState.executeState("2"); // Proposal
        currentState.executeState("4"); // Spades
        currentState.executeState("1, 3"); // P1 en P3 zijn partners
        currentState.executeState("5"); // 5 slagen (Gefaald, 8 nodig)

        assertTrue(game.getPlayers().get(0).getScore() < 0);
        assertTrue(game.getPlayers().get(2).getScore() < 0);
        assertTrue(game.getPlayers().get(1).getScore() > 0);
    }
    @Test
    void testMultipleRoundsConsecutively() {
        game.addPlayer(new Player(new HumanStrategy(), "P1"));
        game.addPlayer(new Player(new HumanStrategy(), "P2"));
        game.addPlayer(new Player(new HumanStrategy(), "P3"));
        game.addPlayer(new Player(new HumanStrategy(), "P4"));

        // Ronde 1: Abondance 10
        currentState = new CountState(game);
        currentState.executeState("");
        currentState.executeState("4");
        currentState.executeState("1");
        currentState.executeState("2");
        currentState.executeState("11");

        int scoreNaRonde1 = game.getPlayers().get(1).getScore();
        currentState.executeState("1"); // Nog een ronde spelen
        currentState = currentState.nextState();
        assertTrue(currentState instanceof CountState);

        // Ronde 2: Miserie
        currentState.executeState("");
        currentState.executeState("7");
        currentState.executeState("1");
        currentState.executeState("2");
        currentState.executeState("2");

        assertEquals(2, game.getRounds().size());
        assertNotEquals(scoreNaRonde1, game.getPlayers().get(1).getScore());
    }
    @Test
    void testOpenMiserieMixedResults() {
        game.addPlayer(new Player(new HumanStrategy(), "P1"));
        game.addPlayer(new Player(new HumanStrategy(), "P2"));
        game.addPlayer(new Player(new HumanStrategy(), "P3"));
        game.addPlayer(new Player(new HumanStrategy(), "P4"));

        currentState = new CountState(game);
        currentState.executeState("");
        currentState.executeState("8"); // Open Miserie
        currentState.executeState("1");
        currentState.executeState("1, 2, 4"); // P1, P2 en P4 spelen miserie
        currentState.executeState("1, 4"); // Enkel P1 en P4 halen 0 slagen

        assertTrue(game.getPlayers().get(0).getScore() > 0); // P1 win
        assertTrue(game.getPlayers().get(1).getScore() < 0); // P2 verlies
        assertTrue(game.getPlayers().get(3).getScore() > 0); // P4 win
    }
    @Test
    void testSoloSlimZeroSum() {
        game.addPlayer(new Player(new HumanStrategy(), "P1"));
        game.addPlayer(new Player(new HumanStrategy(), "P2"));
        game.addPlayer(new Player(new HumanStrategy(), "P3"));
        game.addPlayer(new Player(new HumanStrategy(), "P4"));

        currentState = new CountState(game);
        currentState.executeState("");
        currentState.executeState("10"); // Solo Slim
        currentState.executeState("2");
        currentState.executeState("4"); // P4 speelt
        currentState.executeState("13"); // Alles gewonnen

        int totaalScore = game.getPlayers().stream().mapToInt(Player::getScore).sum();
        assertEquals(0, totaalScore);
        assertTrue(game.getPlayers().get(3).getScore() > 0);

        currentState.executeState("2"); // Stop
        currentState = currentState.nextState();
        assertTrue(currentState instanceof MenuState);
    }
}