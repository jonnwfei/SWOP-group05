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

    /**
     * Runs the full application flow using scripted input.
     */
    private WhistGame runIntegrationTest(String... scriptLines) throws Exception {
        String script = String.join("\n", scriptLines) + "\n";
        System.setIn(new ByteArrayInputStream(script.getBytes()));

        GameController controller = new GameController();

        // Access private game field via reflection
        Field gameField = GameController.class.getDeclaredField("game");
        gameField.setAccessible(true);
        WhistGame game = (WhistGame) gameField.get(controller);

        try {
            controller.run();
        } catch (Exception ignored) {
            // Stops when input runs out
        }

        return game;
    }

    // =========================================================================
    // REQ-COUNT-01 — Abondance 9 success
    // =========================================================================

    @Test
    @DisplayName("REQ-COUNT-01 Steps 1-7: Abondance 9 success updates bidder and keeps zero-sum")
    void testAbondance9Success() throws Exception {
        WhistGame game = runIntegrationTest(
                "2",                    // 1. choose count mode
                "P1", "P2", "P3", "P4",// 2. register players

                // 3–4 handled internally (deal + round setup)

                "3",                    // 5a. choose bid (Abondance 9)
                "2",                    // 5b. choose trump suit
                "2",                    // 6. choose bidder (P2)
                "10"                    // 7. tricks won
        );

        List<Player> players = game.getPlayers();

        assertEquals(4, players.size(), "Game must contain 4 players");

        assertTrue(players.get(1).getScore() > 0,
                "Bidder (P2) should gain points");

        assertEquals(0,
                players.stream().mapToInt(Player::getScore).sum(),
                "Scoring must remain zero-sum");
    }

    // =========================================================================
    // REQ-COUNT-02 — Miserie multiple players
    // =========================================================================

    @Test
    @DisplayName("REQ-COUNT-02 Steps 1-7: Miserie supports multiple participants and explicit winners")
    void testMiserieMixedResults() throws Exception {
        WhistGame game = runIntegrationTest(
                "2",                    // 1. choose count mode
                "P1", "P2", "P3", "P4",// 2. register players

                // 3–4 handled internally

                "9",                    // 5a. choose Miserie                 // 5b. confirm Miserie
                "1,2,3",                // 6. select participants
                "1"                     // 7. select winner(s)
        );

        List<Player> players = game.getPlayers();

        assertTrue(players.get(0).getScore() > 0,
                "P1 is winner and should gain points");

        assertTrue(players.get(1).getScore() < 0,
                "P2 failed Miserie and should lose points");

        assertTrue(players.get(2).getScore() < 0,
                "P3 failed Miserie and should lose points");

        assertEquals(0,
                players.stream().mapToInt(Player::getScore).sum(),
                "Miserie scoring must remain zero-sum");
    }

    // =========================================================================
    // REQ-COUNT-03 — Solo Slim max tricks
    // =========================================================================

    @Test
    @DisplayName("REQ-COUNT-03 Steps 1-7: Solo Slim maximum tricks rewards bidder")
    void testSoloSlimMaximumScore() throws Exception {
        WhistGame game = runIntegrationTest(
                "2",                    // 1. choose count mode
                "P1", "P2", "P3", "P4",// 2. register players

                // 3–4 handled internally

                "10",                   // 5a. choose Solo Slim
                "3",                    // 5b. choose trump suit
                "4",                    // 6. choose bidder (P4)
                "13"                    // 7. tricks won (maximum)
        );

        List<Player> players = game.getPlayers();

        assertEquals(0,
                players.stream().mapToInt(Player::getScore).sum(),
                "Total score must remain zero-sum");

        assertTrue(players.get(3).getScore() > 0,
                "Solo Slim bidder (P4) should gain points on 13 tricks");
    }
}