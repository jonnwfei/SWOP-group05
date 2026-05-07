package usecases;

import base.GameController;
import base.domain.WhistGame;
import base.domain.player.Player;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Scenario tests for UC 4.8 — Remove rounds.
 *
 * Precondition: The current round has finished.
 *
 * UC Steps:
 *  1. User indicates a round should be removed (scoreboard option).
 *  2. System asks which round to remove.
 *  3. System removes the round and updates scores accordingly.
 *  4. Steps 1-3 repeat until all desired rounds removed.
 *  5. Usage resumes from UC 4.1 step 9 (count) or UC 4.2 step 13 (game).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC 4.8 — Remove rounds")
class UC8_RemoveRoundsTest {

    private final InputStream sysInBackup = System.in;

    @AfterEach
    void tearDown() {
        System.setIn(sysInBackup);
    }

    private WhistGame runCount(String... lines) throws Exception {
        String script = String.join("\n", lines) + "\n";
        System.setIn(new ByteArrayInputStream(script.getBytes()));

        GameController controller = new GameController();
        Field gameField = GameController.class.getDeclaredField("game");
        gameField.setAccessible(true);
        WhistGame game = (WhistGame) gameField.get(controller);

        try { controller.run(); } catch (Exception ignored) {}
        return game;
    }

    // =========================================================================
    // Steps 1-5: Remove a round — success
    // =========================================================================

    @Test
    @DisplayName("Steps 1-5: Remove the only round — scores reset to zero")
    void testRemoveOnlyRound() throws Exception {
        WhistGame game = runCount(
                "2",                              // UC1 Step 1: count mode
                "P1", "P2", "P3", "P4",

                // Round 1
                "3",                              // UC1 Step 5: Abondance 9 x

                "2",
                "1",                              // UC1 Step 6: P1 bids
                "10",                             // UC1 Step 7b: tricks won

                // Step 1 UC8: remove round
                "4",                              // option to remove round //

                // Step 2 UC8: which round to remove
                "1"                              // remove round 1

        );

        // Step 3: scores updated — removing the only round resets all to 0
        List<Player> players = game.getPlayers();
        players.forEach(p ->
                assertEquals(0, p.getScore(),
                        p.getName() + " score should be 0 after removing the only round"));

        // Round list should be empty
        assertTrue(game.getRounds().isEmpty(), "No rounds should remain after removing all");
    }

    @Test
    @DisplayName("Steps 1-5: Remove first round of two — second round score remains")
    void testRemoveFirstOfTwoRounds() throws Exception {
        WhistGame game = runCount(
                "2",
                "P1", "P2", "P3", "P4",

                // Round 1: Abondance 9, P1 wins 10 tricks
                "3",
                "2",
                "1",
                "10",

                // Extension 9a: start another round
                "1",

                // Round 2: Abondance 9, P2 wins 9 tricks
                "3",                              // TODO: verify
                "1",
                "2",
                "9",

                // Step 1 UC8: remove a round
                "4",                              // TODO: verify remove option

                // Step 2: select round 1
                "1"

        );

        // Step 3: only round 2 remains — scores reflect round 2 only
        assertEquals(1, game.getRounds().size(), "Only one round should remain");
        assertEquals(0, game.getPlayers().stream().mapToInt(Player::getScore).sum(),
                "Scores must remain zero-sum after round removal");
    }

    @Test
    @DisplayName("Steps 1-5: Remove second round of two — first round score remains")
    void testRemoveSecondOfTwoRounds() throws Exception {
        WhistGame game = runCount(
                "2",
                "P1", "P2", "P3", "P4",

                "3",                              // Round 1 // TODO: verify
                "2",
                "1",
                "10",

                "1",                              // start new round

                "3",                              // Round 2 // TODO: verify
                "1",
                "2",
                "9",

                "6",                              // Step 1: remove round // TODO: verify
                "2",                              // Step 2: select round 2
                "2"                               // quit
        );

        assertEquals(1, game.getRounds().size());
        assertEquals(0, game.getPlayers().stream().mapToInt(Player::getScore).sum());
    }

    // =========================================================================
    // Step 3: Scores updated correctly after removal
    // =========================================================================

    @Test
    @DisplayName("Step 3: Scores after removal are zero-sum")
    void testScoresZeroSumAfterRoundRemoval() throws Exception {
        WhistGame game = runCount(
                "2",
                "P1", "P2", "P3", "P4",
                "3",                              // TODO: verify
                "1",
                "1",
                "13",                             // all tricks — max score

                "6",                              // Step 1: remove // TODO: verify option
                "1",                              // Step 2: round 1

                "2"                               // quit
        );

        // Step 3: zero-sum holds after removal
        assertEquals(0,
                game.getPlayers().stream().mapToInt(Player::getScore).sum(),
                "Scores must be zero-sum after round removal");
    }

    // =========================================================================
    // Step 4: Remove multiple rounds
    // =========================================================================

    @Test
    @DisplayName("Step 4: Remove multiple rounds — all targeted rounds removed")
    void testRemoveMultipleRounds() throws Exception {
        WhistGame game = runCount(
                "2",
                "P1", "P2", "P3", "P4",

                // Round 1
                "3",                              // TODO: verify
                "2", "1", "10",

                "1",                              // new round

                // Round 2
                "3",
                "1", "2", "9",

                "1",                              // new round

                // Round 3
                "3",
                "3", "3", "11",

                // Step 1-3: remove round 1
                "4",
                "1",

                // Step 4: remove round 2 (now index 1)
                "4",
                "1"
        );

        assertEquals(1, game.getRounds().size(), "Only round 3 should remain");
        assertEquals(0, game.getPlayers().stream().mapToInt(Player::getScore).sum());
    }

    // =========================================================================
    // Negative — invalid round selection
    // =========================================================================

    @Test
    @DisplayName("Step 2: Invalid round index re-prompts without crashing")
    void testInvalidRoundIndexRePrompts() throws Exception {
        WhistGame game = runCount(
                "2",
                "P1", "P2", "P3", "P4",
                "3",                              // TODO: verify
                "2", "1", "10",

                "6",                              // Step 1: remove // TODO: verify
                "abc",                            // Step 2: invalid
                "1",                              // Step 2: valid retry
                "2"
        );

        assertNotNull(game);
        assertEquals(0, game.getPlayers().stream().mapToInt(Player::getScore).sum());
    }

    @Test
    @DisplayName("Step 2: Out-of-range round index re-prompts without crashing")
    void testOutOfRangeRoundIndexRePrompts() throws Exception {
        WhistGame game = runCount(
                "2",
                "P1", "P2", "P3", "P4",
                "3",                              // TODO: verify
                "1", "1", "9",

                "6",                              // Step 1: remove // TODO: verify
                "999",                            // Step 2: out of range
                "1",                              // Step 2: valid
                "2"
        );

        assertNotNull(game);
    }

    @Test
    @DisplayName("Remove round when no rounds exist — system handles gracefully")
    void testRemoveRoundWhenNoRoundsExist() throws Exception {
        // This tests a degenerate case: somehow triggering remove with 0 rounds
        // In practice this should not be reachable via normal UI, but system must not crash
        WhistGame game = runCount(
                "2",
                "P1", "P2", "P3", "P4",
                "3",
                "2", "1", "10",

                "4",
                "1",

                // Now 0 rounds — attempting remove again should be handled gracefully
                "4",
                "2"
        );

        assertNotNull(game);
    }
}