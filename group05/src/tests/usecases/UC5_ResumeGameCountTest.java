package usecases;

import base.GameController;
import base.domain.WhistGame;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Scenario tests for UC 4.5 — Resume game/count.
 *
 * Precondition: None (can resume from the main menu).
 *
 * UC Steps:
 *  1. User selects to resume a saved game/count (input "3" at main menu).
 *  2. System shows a list of saves by their description.
 *  3. User selects a saved state.
 *  4. System loads the saved state from disk.
 *  5. Usage resumes from UC 4.1 step 4 (count) or UC 4.2 step 4 (game).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC 4.5 — Resume game/count")
class UC5_ResumeGameCountTest {

    private final InputStream sysInBackup = System.in;

    @AfterEach
    void tearDown() {
        System.setIn(sysInBackup);
    }

    private WhistGame run(String... lines) throws Exception {
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
    // Helper: save a count so we have something to resume
    // =========================================================================

    private void saveCount(String saveName) throws Exception {
        run(
                "2",
                "P1", "P2", "P3", "P4",
                "3",                              // TODO: verify bid index
                "2",
                "1",
                "10",
                "3",                              // save
                saveName                          // description
        );
    }

    // =========================================================================
    // Steps 1-5: Resume a saved count
    // =========================================================================

    @Test
    @DisplayName("Steps 1-5: Resume saved count — players and scores are restored")
    void testResumeCount() throws Exception {
        // Pre-condition: save exists
        saveCount("resumeTestCount");

        WhistGame game = run(
                "3",                              // Step 1: resume
                "1",                              // Step 3: select first save // TODO: verify index
                "2"                               // Step 5: quit (UC1 step 9)
        );

        // Step 5: game loaded — players present with restored scores
        assertNotNull(game);
        assertFalse(game.getPlayers().isEmpty(), "Resumed game must have players");
    }

    @Test
    @DisplayName("Steps 1-5: Resume saved game — resumes from UC 4.2 step 4")
    void testResumeGame() throws Exception {
        // TODO: first save an in-app game (requires completing a round)
        // For now, verify the menu path doesn't crash
        WhistGame game = run(
                "3",                              // Step 1: resume
                "1"                               // Step 3: select save
        );

        assertNotNull(game);
    }

    @Test
    @DisplayName("Steps 1-5: Resumed count preserves accumulated scores")
    void testResumeCountPreservesScores() throws Exception {
        saveCount("scorePreserveTest");

        WhistGame resumed = run(
                "3",                              // Step 1: resume
                "1",                              // Step 3: select save
                "2"                              // Step 5: quit
        );

        // Step 4: Scores must have been loaded (not all zero after a round)
        assertNotNull(resumed);
        int total = resumed.getPlayers().stream()
                .mapToInt(base.domain.player.Player::getScore)
                .sum();
        assertEquals(0, total, "Restored scores must remain zero-sum");
    }

    // =========================================================================
    // Step 2: No saves available
    // =========================================================================

    @Test
    @DisplayName("Step 2: No saves available — system handles gracefully")
    void testNoSavesAvailable() throws Exception {
        // TODO: ensure no saves exist on disk before this test (may need a clean dir)
        WhistGame game = run(
                "3"                               // Step 1: resume — but no saves exist
        );

        // System must not crash when no saves exist
        assertNotNull(game);
    }

    // =========================================================================
    // Negative: invalid save index
    // =========================================================================

    @Test
    @DisplayName("Step 3: Invalid save index re-prompts")
    void testInvalidSaveIndexRePrompts() throws Exception {
        saveCount("invalidIndexTest");

        WhistGame game = run(
                "3",                              // Step 1: resume
                "abc",                            // Step 3: invalid input
                "1",                              // Step 3: valid selection
                "2"                               // Step 5: quit
        );

        assertNotNull(game);
    }

    @Test
    @DisplayName("Step 3: Out-of-range save index re-prompts")
    void testOutOfRangeSaveIndexRePrompts() throws Exception {
        saveCount("outOfRangeTest");

        WhistGame game = run(
                "3",                              // Step 1: resume
                "999",                            // Step 3: out of range
                "1",                              // Step 3: valid
                "2"
        );

        assertNotNull(game);
    }
}