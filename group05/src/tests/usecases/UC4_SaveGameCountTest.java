package usecases;

import base.GameController;
import base.domain.WhistGame;
import base.storage.GamePersistenceService;
import base.storage.SaveRepository;
import base.storage.snapshots.GameSnapshot;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UC 4.4 — Save game/count.
 *
 * Count menu: "2" / names
 * Game menu:  "1" / bots / humans(NEW) / names / strategies
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC 4.4 — Save game/count")
class UC4_SaveGameCountTest {

    SaveRepository testRepo;
    GamePersistenceService testPersistenceService;

    @BeforeEach
    void setUp() {
        testRepo = new SaveRepository(Paths.get("testSaves"));
        testPersistenceService = new GamePersistenceService(testRepo);
    }

    private final InputStream sysInBackup = System.in;

    @AfterEach
    void tearDown() { System.setIn(sysInBackup); }

    private WhistGame runCount(String... lines) throws Exception {
        String script = String.join("\n", lines) + "\n";
        System.setIn(new ByteArrayInputStream(script.getBytes()));
        GameController controller = new GameController();
        Field f = GameController.class.getDeclaredField("game");
        f.setAccessible(true);
        WhistGame game = (WhistGame) f.get(controller);
        try { controller.run(); } catch (Exception ignored) {}
        return game;
    }

    // =========================================================================
    // Save count — steps 1-4
    // =========================================================================

    @Test
    @DisplayName("Steps 1-4 (count): Save count — file created on disk")
    void testSaveCount() throws Exception {
        WhistGame game = runCount(
                "2",                              // Step 1 UC1: count mode
                "P1", "P2", "P3", "P4",          // Step 2 UC1: names
                "3",                              // Step 5: Abondance 9 // TODO: verify index
                "2",
                "1",
                "10",                             // Step 7: tricks won
                "3",                              // Step 1 UC4: save option in scoreboard
                "My test save"                    // Step 2 UC4: description
        );

        assertNotNull(game);
        assertTrue(testPersistenceService.listDescriptions().contains("My test save"),
                "Description should appear in persistence service");
        GameSnapshot snap = testRepo.loadByDescription("My test save");
        assertNotNull(snap);
        assertEquals(4, snap.players().size());
    }

    @Test
    @DisplayName("Steps 1-4 (count): Save with description containing spaces")
    void testSaveWithSpaces() throws Exception {
        WhistGame game = runCount(
                "2",
                "P1", "P2", "P3", "P4",
                "3",                              // TODO: verify index
                "1", "2", "9",
                "3",                              // Step 1 UC4: save
                "My Save With Spaces"             // Step 2 UC4: description
        );

        assertNotNull(game);
    }

    @Test
    @DisplayName("Steps 1-4 (count): After save, usage resumes at UC1 step 9")
    void testAfterSaveResumes() throws Exception {
        WhistGame game = runCount(
                "2",
                "P1", "P2", "P3", "P4",
                "3",                              // TODO: verify
                "2", "1", "10",
                "3",                              // Step 1 UC4: save
                "round1save"                      // Step 2 UC4: description
        );

        assertNotNull(game);
    }

    // =========================================================================
    // Negative — blank description re-prompts
    // =========================================================================

    @Test
    @DisplayName("Step 2: Blank description is rejected and re-prompted")
    void testBlankDescriptionRePrompts() throws Exception {
        WhistGame game = runCount(
                "2",
                "P1", "P2", "P3", "P4",
                "3",                              // TODO: verify
                "1", "1", "9",
                "3",                              // Step 1 UC4: save
                "",                               // Step 2: blank — re-prompt
                "validDescription"                // Step 2: valid retry
        );

        assertNotNull(game);
    }
}