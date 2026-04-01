package usecases;

import base.GameController;
import base.domain.WhistGame;
import base.domain.player.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("REQ-COUNT-01 Steps 1-6: Abondance 9 success updates bidder and keeps zero-sum")
    void testAbondance9Success() throws Exception {
        WhistGame game = runIntegrationTest(
                // Step 1 - choose count mode
                "2", "John 1", "John 2", "John 3", "John 4",
                // Step 2 - choose bid and suit
                "3", "2", "2", "10");

        List<Player> players = game.getPlayers();
        assertTrue(players.get(1).getScore() > 0, "Bidder P2 should win on 10 tricks in Abondance 9");
        assertEquals(0, players.stream().mapToInt(Player::getScore).sum(), "Score calculation must remain zero-sum");
    }

    @Test
    @DisplayName("REQ-COUNT-02 Steps 1-6: Miserie supports multiple participants and explicit winners")
    void testMiserieMixedResults() throws Exception {
        WhistGame game = runIntegrationTest(
                "2", "P1", "P2", "P3", "P4",
                "7", "1", "1, 2, 3", "1");

        List<Player> players = game.getPlayers();
        assertTrue(players.get(0).getScore() > 0, "P1 is listed as winner and should gain points");
        assertTrue(players.get(1).getScore() < 0, "P2 failed miserie and should lose points");
        assertTrue(players.get(2).getScore() < 0, "P3 failed miserie and should lose points");
        assertEquals(0, players.stream().mapToInt(Player::getScore).sum(), "Miserie scoring must remain zero-sum");
    }

    @Test
    @DisplayName("REQ-COUNT-03 Steps 1-6: Solo Slim maximum tricks rewards bidder")
    void testSoloSlimMaximumScore() throws Exception {
        WhistGame game = runIntegrationTest(
                "2", "P1", "P2", "P3", "P4",
                "10", "3", "4", "13");

        assertEquals(0, game.getPlayers().stream().mapToInt(Player::getScore).sum());
        assertTrue(game.getPlayers().get(3).getScore() > 0, "Solo Slim bidder should win on 13 tricks");
    }

    @Test
    @DisplayName("REQ-COUNT-04 Steps 7-8a: restart count mode keeps accumulated scores")
    void testRestartExtensionKeepScores() throws Exception {
        WhistGame game = runIntegrationTest(
                "2", "P1", "P2", "P3", "P4",
                // Round 1
                "3", "2", "1", "10",
                // Step 8a - restart count flow
                "1",
                // Round 2
                "9", "1", "2", "13");

        assertEquals(2, game.getRounds().size());
        assertNotEquals(0, game.getPlayers().get(0).getScore(), "Round 1 score should be preserved after restart");
        assertTrue(game.getPlayers().get(1).getScore() > 0, "Round 2 score should be added on top");
        assertEquals(0, game.getPlayers().stream().mapToInt(Player::getScore).sum(), "Total score must stay zero-sum");
    }
}