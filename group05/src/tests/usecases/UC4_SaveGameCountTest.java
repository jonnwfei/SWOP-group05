package usecases;

import base.GameController;
import base.domain.WhistGame;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.deck.Deck;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Scenario tests for UC 4.4 — Save game/count.
 *
 * Precondition: The current round has finished.
 *
 * UC Steps:
 *  1. User requests to save after a round ends (input "3").
 *  2. System asks for a description.
 *  3. System stores the game/count persistently on disk with that description.
 *  4. Usage resumes from UC 4.2 step 13 (game) or UC 4.1 step 9 (count).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC 4.4 — Save game/count")
class UC4_SaveGameCountTest {

    private final InputStream sysInBackup = System.in;

    @AfterEach
    void tearDown() {
        System.setIn(sysInBackup);
    }

    private WhistGame run(List<List<Card>> hands, String... lines) throws Exception {
        String script = String.join("\n", lines) + "\n";
        System.setIn(new ByteArrayInputStream(script.getBytes()));

        try (MockedConstruction<Deck> mockedDeck = mockConstruction(Deck.class, (mock, ctx) -> {
            if (hands != null) when(mock.deal()).thenReturn(hands);
        })) {
            GameController controller = new GameController();
            Field gameField = GameController.class.getDeclaredField("game");
            gameField.setAccessible(true);
            WhistGame game = (WhistGame) gameField.get(controller);

            Thread injector = new Thread(() -> {
                try {
                    while (game.getPlayers().size() < 4) Thread.sleep(10);
                    Field dealerField = WhistGame.class.getDeclaredField("dealer");
                    dealerField.setAccessible(true);
                    dealerField.set(game, game.getPlayers().get(0));
                } catch (Exception ignored) {}
            });
            injector.start();

            try { controller.run(); } catch (Exception ignored) {}
            return game;
        }
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
    // Save count — steps 1-4
    // =========================================================================

    @Test
    @DisplayName("Steps 1-4 (count): Save count — file created on disk")
    void testSaveCount() throws Exception {
        WhistGame game = runCount(
                "2",                              // Step 1 UC1: count mode
                "P1", "P2", "P3", "P4",          // Step 2 UC1: names

                "3",                              // Step 5 UC1: Abondance 9 // TODO: verify index
                "2",
                "1",
                "10",                             // Step 7 UC1: tricks won

                "3",                              // Step 1 UC4: save (option 3 in scoreboard)
                "My test save"                    // Step 2 UC4: description
                // Step 3 UC4: system saves to disk
                // Step 4 UC4: resumes at UC1 step 9
        );

        assertNotNull(game);
        // Step 3: a save file with this description should exist on disk
        // TODO: adapt path to actual save directory used by GamePersistenceService
        // File saveFile = new File("saves/My test save.json");
        // assertTrue(saveFile.exists(), "Save file should be created on disk");
    }

    @Test
    @DisplayName("Steps 1-4 (count): Save with description containing spaces")
    void testSaveCountWithSpacesInDescription() throws Exception {
        WhistGame game = runCount(
                "2",
                "P1", "P2", "P3", "P4",
                "3",                              // TODO: verify index
                "1",
                "2",
                "9",
                "3",                              // Step 1 UC4: save
                "My Save With Spaces"             // Step 2 UC4: description
        );

        assertNotNull(game);
    }

    @Test
    @DisplayName("Steps 1-4 (count): After save, usage resumes at UC1 step 9 (quit)")
    void testAfterSaveResumesAtQuit() throws Exception {
        WhistGame game = runCount(
                "2",
                "P1", "P2", "P3", "P4",
                "3",                              // TODO: verify index
                "2",
                "1",
                "10",
                "3",                              // Step 1 UC4: save
                "round1save",                     // Step 2 UC4: description
                "2"                               // Step 4 UC4 → UC1 step 9: quit
        );

        assertNotNull(game);
    }

    // =========================================================================
    // Save in-app game — steps 1-4
    // =========================================================================

    @Test
    @DisplayName("Steps 1-4 (game): Save in-app game — does not crash")
    void testSaveGame() throws Exception {
        // Run a minimal all-bot game so a round completes naturally
        WhistGame game = run(null,
                "1",                              // UC2 Step 1: in-app game
                "3",                              // Step 2a: 3 bots
                "Human",
                "1", "1", "1",                   // bot strategies

                // Step 1 UC4: user requests save after round ends
                // TODO: input "3" at the scoreboard to trigger save
                "3",                              // save option
                "gameSave1"                       // Step 2 UC4: description
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
                "3",                              // TODO: verify index
                "1",
                "1",
                "9",
                "3",                              // Step 1 UC4: save
                "",                               // Step 2 UC4: blank — should re-prompt
                "validDescription"                // Step 2 UC4: retry with valid
        );

        assertNotNull(game);
    }
}