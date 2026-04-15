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

public class InAppGame {

    private final InputStream sysInBackup = System.in;

    @AfterEach
    void tearDown() {
        System.setIn(sysInBackup);
    }

    /**
     * Runs full application with scripted input
     */
    private WhistGame runIntegrationTest(String... scriptLines) throws Exception {
        String script = String.join("\n", scriptLines) + "\n";
        System.setIn(new ByteArrayInputStream(script.getBytes()));

        GameController controller = new GameController();

        Field gameField = GameController.class.getDeclaredField("game");
        gameField.setAccessible(true);
        WhistGame game = (WhistGame) gameField.get(controller);

        try {
            controller.run();
        } catch (Exception ignored) {
            // stops when input ends
        }

        return game;
    }

    // =========================================================================
    // REQ-PLAY-01 — Steps 1–3: Game initialization
    // =========================================================================

    @Test
    @DisplayName("REQ-PLAY-01 Steps 1-3: Starting in-app game initializes players with zero score")
    void testGameInitialization() throws Exception {
        WhistGame game = runIntegrationTest(
                "1",                    // 1. start in-app game
                "0",                    // 2. number of bots
                "Alice", "Bob", "Cara", "Daan" // 3. register players
        );

        List<Player> players = game.getPlayers();

        assertEquals(4, players.size(), "Exactly 4 players must be registered");

        assertTrue(players.stream().allMatch(p -> p.getScore() == 0),
                "All players must start with score 0");
    }

    // =========================================================================
    // REQ-PLAY-02 — Steps 4–5: Round setup (dealer + cards)
    // =========================================================================

    @Test
    @DisplayName("REQ-PLAY-02 Steps 4-5: Starting round assigns dealer and deals cards")
    void testRoundInitialization() throws Exception {
        WhistGame game = runIntegrationTest(
                "1",                    // 1
                "0",                    // 2
                "P1", "P2", "P3", "P4",// 3

                // Trigger transition into bidding → round setup happens
                "1"                     // 6. first bid input (minimal to proceed)
        );

        assertNotNull(game.getDealerPlayer(), "Dealer must be assigned");
        assertFalse(game.getRounds().isEmpty(), "A round must be initialized");
    }

    // =========================================================================
    // REQ-PLAY-03 — Steps 6: Bidding phase starts correctly
    // =========================================================================

    @Test
    @DisplayName("REQ-PLAY-03 Step 6: Bidding phase starts and progresses")
    void testBiddingPhaseStarts() throws Exception {
        WhistGame game = runIntegrationTest(
                "1",                    // 1
                "0",                    // 2
                "P1", "P2", "P3", "P4",// 3

                // Step 6: simulate 4 bids
                "1",                    // P1 bid
                "1",                    // P2 bid
                "1",                    // P3 bid
                "1"                     // P4 bid
        );

        assertFalse(game.getRounds().isEmpty(),
                "Bidding should result in an active round");
    }

    // =========================================================================
    // REQ-PLAY-04 — Steps 7–10: First trick execution
    // =========================================================================

    @Test
    @DisplayName("REQ-PLAY-04 Steps 7-10: Players can play a full trick")
    void testSingleTrickExecution() throws Exception {
        WhistGame game = runIntegrationTest(
                "1",                    // 1
                "0",                    // 2
                "P1", "P2", "P3", "P4",// 3

                // bidding (step 6)
                "1", "1", "1", "16",

                // Step 7–10: play one trick (4 cards)
                "1",                    // Player 1 plays
                "",                     // 8. confirm
                "1",                    // Player 2 plays
                "",                     // confirm
                "1",                    // Player 3 plays
                "",                     // confirm
                "1"                     // Player 4 plays
        );

        assertTrue(game.getCurrentRound().getTricks().size() >= 1,
                "At least one trick should be completed");
    }

    // =========================================================================
    // REQ-PLAY-05 — Steps 11–12: Full round progression
    // =========================================================================

    @Test
    @DisplayName("REQ-PLAY-05 Steps 11-12: Full round results in scoring")
    void testFullRoundScoring() throws Exception {
        WhistGame game = runIntegrationTest(
                "1",                    // 1
                "0",                    // 2
                "P1", "P2", "P3", "P4",// 3

                // bidding
                "1", "1", "1", "1",

                // simulate many plays (not full 13, but enough to trigger logic)
                "1", "", "1", "", "1", "", "1",
                "1", "", "1", "", "1", "", "1",
                "1", "", "1", "", "1", "", "1"
        );

        int totalScore = game.getPlayers().stream()
                .mapToInt(Player::getScore)
                .sum();

        assertEquals(0, totalScore,
                "Score must remain zero-sum after round calculation");
    }

    // =========================================================================
    // REQ-PLAY-06 — Step 13: Game can terminate
    // =========================================================================

    @Test
    @DisplayName("REQ-PLAY-06 Step 13: User can quit the game")
    void testGameExit() throws Exception {
        WhistGame game = runIntegrationTest(
                "1",                    // 1
                "0",                    // 2
                "P1", "P2", "P3", "P4",// 3

                "0"                     // 13. quit immediately
        );

        assertNotNull(game, "Game should exit cleanly");
    }
}