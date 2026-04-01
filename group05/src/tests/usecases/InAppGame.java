package usecases;

import base.GameController;
import base.domain.WhistGame;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class InAppGame {
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
    @DisplayName("REQ-PLAY-01 Steps 1-4: menu setup in play mode registers 4 players")
    void playModeSetupRegistersPlayers() throws Exception {
        WhistGame game = runIntegrationTest(
                // Step 1 - choose play mode
                "1",
                // Step 2 - choose number of bots
                "0",
                // Step 3 - enter four player names
                "Alice", "Bob", "Cara", "Daan");

        assertEquals(4, game.getPlayers().size(), "Exactly 4 players must be registered");
        assertEquals("Alice", game.getPlayers().get(0).getName());
        assertEquals("Daan", game.getPlayers().get(3).getName());
    }

    @Test
    @DisplayName("REQ-PLAY-02 Steps 5-6: entering bid phase creates the first round")
    void playModeTransitionsToBidRoundAfterMenuFlow() throws Exception {
        WhistGame game = runIntegrationTest(
                "1",
                "1",
                "Alice", "Bob", "Cara",
                "1");

        // After menu completion, controller transitions into BidState which initializes
        // round data.
        assertEquals(4, game.getPlayers().size(), "Menu flow must still produce a 4-player table");
        assertFalse(game.getRounds().isEmpty(), "Entering bid phase should initialize the first round");
        assertNotNull(game.getDealerPlayer(), "Dealer must be set before bidding starts");
    }
}
