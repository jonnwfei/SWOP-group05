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
 * Scenario tests for UC 4.6 — Add player.
 *
 * Precondition: The current round has finished.
 *
 * UC Steps:
 *  1. User indicates to add a player (scoreboard option "4").
 *  2. System asks for the name of the player.
 *  3. System adds the player with score = 0.
 *  4. Steps 1-3 repeat until desired number of players reached.
 *  5. Usage resumes from UC 4.1 step 9 (count) or UC 4.2 step 13 (game).
 *
 * Extension 2a: In-app game — system asks if player is a bot.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC 4.6 — Add player")
class UC6_AddPlayerTest {

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
    // Steps 1-5: Add a single player
    // =========================================================================

    @Test
    @DisplayName("Steps 1-5: Add one player to count — player added with score 0")
    void testAddOnePlayerToCount() throws Exception {
        WhistGame game = runCount(
                "2",                              // UC1 Step 1: count mode
                "P1", "P2", "P3", "P4",          // UC1 Step 2: initial 4 players

                // First round
                "3",                              // UC1 Step 5: Abondance 9 // TODO: verify index
                "2",
                "1",
                "10",

                // Step 1 UC6: add player
                "5",                              // scoreboard option 4 = add player
                // Step 2 UC6: name
                "NewPlayer"
        );

        // Step 3: new player present with score 0
        List<Player> players = game.getPlayers();
        assertTrue(players.size() > 4, "Player list should grow after add");

        Player newPlayer = players.stream()
                .filter(p -> p.getName().equals("NewPlayer"))
                .findFirst()
                .orElse(null);
        assertNotNull(newPlayer, "NewPlayer should be in the game");
        assertEquals(0, newPlayer.getScore(), "New player starts at score 0");
    }

    @Test
    @DisplayName("Steps 1-5: Add player — new player has score of 0 regardless of others")
    void testNewPlayerAlwaysStartsAtZero() throws Exception {
        WhistGame game = runCount(
                "2",
                "P1", "P2", "P3", "P4",
                "3",
                "1",
                "2",
                "13",                             // P2 wins a lot — has non-zero score

                "5",                              // Step 1: add player
                "Newcomer"                      // Step 2: name
                                            // Step 5: quit
        );

        Player newcomer = game.getPlayers().stream()
                .filter(p -> p.getName().equals("Newcomer"))
                .findFirst()
                .orElse(null);

        assertNotNull(newcomer);
        assertEquals(0, newcomer.getScore(), "Newcomer must start at exactly 0");
    }

    // =========================================================================
    // Step 4: Add multiple players sequentially
    // =========================================================================

    @Test
    @DisplayName("Step 4: Add two players sequentially — both added with score 0")
    void testAddTwoPlayersSequentially() throws Exception {
        WhistGame game = runCount(
                "2",
                "P1", "P2", "P3", "P4",
                "3",                              // TODO: verify index
                "2",
                "1",
                "9",

                // First add
                "5",                              // Step 1 UC6: add player
                "Fifth",                          // Step 2 UC6: name

                // Step 4: repeat
                "5",                              // Step 1 UC6: add again
                "Sixth"                          // Step 2 UC6: name


        );

        List<Player> players = game.getPlayers();
        assertEquals(6, players.size(), "Should have 6 players after adding 2");

        players.stream()
                .filter(p -> p.getName().equals("Fifth") || p.getName().equals("Sixth"))
                .forEach(p -> assertEquals(0, p.getScore(), p.getName() + " must start at 0"));
    }

    // =========================================================================
    // Extension 2a: In-app game — ask if bot
    // =========================================================================

    @Test
    @DisplayName("Extension 2a: Add human player during in-app game")
    void testAddHumanPlayerInGame() throws Exception {
        // TODO: trigger this after a round completes in an in-app game
        // The scoreboard in game mode should also show option 4
        WhistGame game = runCount(
                "2",
                "P1", "P2", "P3", "P4",
                "3",                              // TODO: verify index
                "2",
                "1",
                "10",
                "5",                              // Step 1: add player
                "ExtraHuman"                      // Step 2: human name
                // Extension 2a: TODO ask if bot? (for game mode only)
        );

        assertNotNull(game);
        assertTrue(game.getPlayers().stream().anyMatch(p -> p.getName().equals("ExtraHuman")));
    }

    // =========================================================================
    // Negative — blank name is rejected
    // =========================================================================
    /**
    @Test
    @DisplayName("Step 2: Blank player name is rejected and re-prompted")
    void testBlankPlayerNameRejected() throws Exception {
        WhistGame game = runCount(
                "2",
                "P1", "P2", "P3", "P4",
                "3",
                "1",
                "1",
                "9",
                "5",                              // Step 1: add player
                "",                               // Step 2: blank — should re-prompt
                "ValidName"                      // Step 2: valid retry

        );

        assertNotNull(game);
        assertTrue(game.getPlayers().stream().anyMatch(p -> p.getName().equals("ValidName")),
                "ValidName should be present after retry");
        assertFalse(game.getPlayers().stream().anyMatch(p -> p.getName().isBlank()),
                "No player should have a blank name");
    }

     */
}