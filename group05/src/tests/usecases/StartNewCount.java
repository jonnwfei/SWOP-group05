package usecases;

import base.GameController;
import base.domain.WhistGame;
import base.domain.player.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StartNewCount {

    private final InputStream sysInBackup = System.in;

    @AfterEach
    void tearDown() {
        System.setIn(sysInBackup);
    }


    private WhistGame runIntegrationTest(String... scriptLines) throws Exception {
        String script = String.join("\n", scriptLines) + "\n";
        System.setIn(new ByteArrayInputStream(script.getBytes()));

        GameController controller = new GameController();

        // Gebruik reflectie om toegang te krijgen tot de private WhistGame game;
        Field gameField = GameController.class.getDeclaredField("game");
        gameField.setAccessible(true);
        WhistGame game = (WhistGame) gameField.get(controller);

        try {
            controller.run();
        } catch (Exception e) {
            // De controller stopt wanneer de gesimuleerde input op is.
        }
        return game;
    }

    @Test
    void testAbondance9Success() throws Exception {
        // Menu -> Namen -> Abondance 9 (3) -> Clubs (2) -> Player 2 (2) -> 10 slagen
        WhistGame game = runIntegrationTest(
                "2", "John 1", "John 2", "John 3", "John 4",
                "3", "2", "2", "10"
        );

        List<Player> players = game.getPlayers();
        assertTrue(players.get(1).getScore() > 0, "De bieder (P2) moet winnen");
        assertEquals(0, players.stream().mapToInt(Player::getScore).sum(), "Zero-sum check");
    }

    @Test
    void testMiserieMixedResults() throws Exception {
        // Menu -> Namen -> Miserie (7) -> Hearts (1) -> P1, P2 & P3 (1, 2, 3) -> Alleen P1 wint (1)
        WhistGame game = runIntegrationTest(
                "2", "P1", "P2", "P3", "P4",
                "7", "1", "1, 2, 3", "1"
        );

        List<Player> players = game.getPlayers();
        assertTrue(players.get(0).getScore() > 0, "P1 haalde miserie");
        assertTrue(players.get(1).getScore() < 0, "P2 faalde");
        assertTrue(players.get(2).getScore() < 0, "P3 faalde");
    }

    @Test
    void testProposalWithPartnerFailure() throws Exception {
        // Menu -> Namen -> Proposal (2) -> Spades (4) -> P1 & P3 partners (1, 3) -> 5 slagen (verlies)
        WhistGame game = runIntegrationTest(
                "2", "A", "B", "C", "D",
                "2", "4", "1, 3", "5"
        );

        List<Player> players = game.getPlayers();
        assertTrue(players.get(0).getScore() < 0, "Partner 1 verliest");
        assertTrue(players.get(2).getScore() < 0, "Partner 2 verliest");
        assertTrue(players.get(1).getScore() > 0, "Tegenstander wint");
    }

    @Test
    void testSoloSlimMaximumScore() throws Exception {
        // Menu -> Namen -> Solo Slim (10) -> Diamonds (3) -> P4 (4) -> 13 slagen
        WhistGame game = runIntegrationTest(
                "2", "P1", "P2", "P3", "P4",
                "10", "3", "4", "13"
        );

        assertEquals(0, game.getPlayers().stream().mapToInt(Player::getScore).sum());
        assertTrue(game.getPlayers().get(3).getScore() > 0, "Solo Slim bieder wint alles");
    }

    @Test
    void testSequentialRoundsAccumulation() throws Exception {
        // Ronde 1: Abondance 9 (P1 wint) -> Opnieuw (1) -> Ronde 2: Miserie (P1 verliest)
        WhistGame game = runIntegrationTest(
                "2", "P1", "P2", "P3", "P4",
                "3", "2", "1", "10", "1",
                "7", "1", "1", "1"
        );

        assertEquals(2, game.getRounds().size());
        // We checken of de score veranderd is na de tweede ronde (P1 wint eerst veel, verliest dan weer)
        assertNotEquals(0, game.getPlayers().getFirst().getScore());
        assertEquals(0, game.getPlayers().stream().mapToInt(Player::getScore).sum());
    }

    @Test
    void testProposalWithPartnerSuccess() throws Exception {
        // Test Case: Proposal met partner (Stap 4 & 5)
        WhistGame game = runIntegrationTest(
                "2", "P1", "P2", "P3", "P4", // Stap 1, 2, 3
                "2", "1", "1, 3", "9"         // Proposal (2), Hearts (1), P1 & P3 (1,3), 9 slagen (Stap 6b)
        );
        assertTrue(game.getPlayers().get(0).getScore() > 0);
        assertTrue(game.getPlayers().get(2).getScore() > 0);
        assertEquals(0, game.getPlayers().stream().mapToInt(Player::getScore).sum());
    }

    @Test
    void testAbondanceFailure() throws Exception {
        // Test Case: Abondance 10 verlies (Stap 4)
        WhistGame game = runIntegrationTest(
                "2", "P1", "P2", "P3", "P4",
                "4", "3", "2", "8"            // Abondance 10 (4), Diamonds (3), P2 speelt (2), 8 slagen (Stap 6b)
        );
        assertTrue(game.getPlayers().get(1).getScore() < 0);
    }


    @Test
    void testSoloSlimMaximum() throws Exception {
        // Test Case: Solo Slim (Stap 4)
        WhistGame game = runIntegrationTest(
                "2", "P1", "P2", "P3", "P4",
                "10", "4", "3", "13"          // Solo Slim (10), Spades (4), P3 speelt, 13 slagen
        );
        assertTrue(game.getPlayers().get(2).getScore() > 0);
    }

    @Test
    void testRestartExtensionKeepScores() throws Exception {
        // Test Case: Extension 8a - Ronde herstarten bij stap 3 met behoud van scores
        WhistGame game = runIntegrationTest(
                "2", "P1", "P2", "P3", "P4",
                "3", "2", "1", "10",          // Ronde 1: Abondance 9 (P1 wint)
                "1",                          // Stap 8a: Restart game (scores kept)
                "9", "1", "2", "13"            // Ronde 2: Solo (P2 wint)
        );

        assertEquals(2, game.getRounds().size());
        assertTrue(game.getPlayers().get(0).getScore() != 0, "Scores van ronde 1 zijn behouden");
        assertTrue(game.getPlayers().get(1).getScore() > 0, "Scores van ronde 2 zijn toegevoegd");
        assertEquals(0, game.getPlayers().stream().mapToInt(Player::getScore).sum(), "Totaal blijft zero-sum");
    }
    @Test
    void testMiserieTotalFailureAndQuit() throws Exception {
        // Menu -> Names -> Miserie (7) -> Hearts (1) -> P1 & P2 (1, 2) -> Everyone loses (-1) -> Quit (2)
        WhistGame game = runIntegrationTest(
                "2", "P1", "P2", "P3", "P4",
                "7","1, 2", "-1"
        );

        assertTrue(game.getPlayers().get(0).getScore() < 0, "P1 should lose points");
        assertTrue(game.getPlayers().get(1).getScore() < 0, "P2 should lose points");
        assertEquals(0, game.getPlayers().stream().mapToInt(Player::getScore).sum());
    }
}