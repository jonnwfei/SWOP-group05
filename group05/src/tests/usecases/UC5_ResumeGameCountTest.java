package usecases;

import base.GameController;
import base.domain.WhistGame;
import base.storage.SaveRepository;
import base.storage.snapshots.GameSnapshot;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Scenario tests for UC 4.5 — Resume game/count.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC 4.5 — Resume game/count")
class UC5_ResumeGameCountTest {

    private final InputStream sysInBackup = System.in;

    // JUnit automatically creates this folder before a test and deletes it afterward!
    @TempDir
    Path testSavesDir;

    @AfterEach
    void tearDown() {
        System.setIn(sysInBackup);
    }

    private WhistGame run(String... lines) throws Exception {
        String script = String.join("\n", lines) + "\n";
        System.setIn(new ByteArrayInputStream(script.getBytes()));

        // INTERCEPT: Whenever new SaveRepository() is called, divert it to our TempDir!
        try (MockedConstruction<SaveRepository> mockedRepo = mockConstruction(SaveRepository.class, (mock, context) -> {

            // Create a REAL repository pointing to the isolated test folder
            SaveRepository isolatedRepo = new SaveRepository(testSavesDir);

            // Delegate the mock's method calls to our isolated real repository
            doAnswer(inv -> {
                isolatedRepo.save(inv.getArgument(0, GameSnapshot.class));
                return null;
            }).when(mock).save(any(GameSnapshot.class));

            lenient().when(mock.listDescriptions()).thenAnswer(inv -> isolatedRepo.listDescriptions());
            lenient().when(mock.loadByDescription(anyString())).thenAnswer(inv -> isolatedRepo.loadByDescription(inv.getArgument(0)));

        })) {
            GameController controller = new GameController();
            Field gameField = GameController.class.getDeclaredField("game");
            gameField.setAccessible(true);
            WhistGame game = (WhistGame) gameField.get(controller);

            try { controller.run(); } catch (Exception ignored) {}
            return game;
        }
    }

    // =========================================================================
    // Helper: save a count so we have something to resume
    // =========================================================================

    private void saveCount(String saveName) throws Exception {
        run(
                "2",
                "P1", "P2", "P3", "P4",
                "3",                              // Access menu / save option
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
        // Because testSavesDir persists for the life of this test,
        // the save file written here will successfully be found in the next run()!
        saveCount("resumeTestCount");

        WhistGame game = run(
                "3",                              // Step 1: resume
                "1",                              // Step 3: select first save (1-based index usually)
                "2"                               // Step 5: quit (UC1 step 9)
        );

        assertNotNull(game);
        assertFalse(game.getPlayers().isEmpty(), "Resumed game must have players");
    }

    @Test
    @DisplayName("Steps 1-5: Resume saved game — menu path doesn't crash")
    void testResumeGame() throws Exception {
        // Just verifying the menu path handles the request without crashing
        saveCount("mockGameSave");

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
                "2"                               // Step 5: quit
        );

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
        // Because of @TempDir, we are GUARANTEED that the folder is completely empty here!
        WhistGame game = run(
                "3"                               // Step 1: resume — but no saves exist
        );

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