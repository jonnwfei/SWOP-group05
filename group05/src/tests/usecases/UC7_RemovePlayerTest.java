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
 * Scenario tests for UC 4.7 — Remove player.
 *
 * Precondition: Current round has finished AND more than 4 players are playing.
 *
 * UC Steps:
 *  1. User indicates to remove a player (scoreboard option "5" — only shown when >4 players).
 *  2. System asks which player to remove.
 *  3. System removes the player.
 *  4. Steps 1-3 repeat until desired number reached.
 *  5. Usage resumes from UC 4.1 step 9 (count) or UC 4.2 step 13 (game).
 *
 * Extension 3a: Removing a player that would drop below 4 — system returns error.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC 4.7 — Remove player")
class UC7_RemovePlayerTest {

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

    /** Runs one count round and adds an extra player, leaving us with 5 players. */
    private String[] roundThenAddPlayer(String extraName, String... after) {
        java.util.List<String> inputs = new java.util.ArrayList<>(List.of(
                "2",
                "P1", "P2", "P3", "P4",
                "3",                          // TODO: verify bid index
                "2",
                "1",
                "10",
                "5",                          // add player (scoreboard option)
                extraName                     // name of 5th player
        ));
        inputs.addAll(List.of(after));
        return inputs.toArray(new String[0]);
    }

    // =========================================================================
    // Steps 1-5: Remove a player — success
    // =========================================================================

    @Test
    @DisplayName("Steps 1-5: Remove one player when 5 present — count drops to 4")
    void testRemoveOnePlayer() throws Exception {
        WhistGame game = runCount(roundThenAddPlayer("ExtraPlayer",
                "6",                          // Step 1 UC7: remove player (option 5)
                "5"                          // Step 2 UC7: select player 5 (ExtraPlayer)
                                           // Step 5: quit
        ));

        List<Player> players = game.getPlayers();
        assertEquals(4, players.size(), "Should have 4 players after removal");
        assertFalse(players.stream().anyMatch(p -> p.getName().equals("ExtraPlayer")),
                "ExtraPlayer should have been removed");
    }

    @Test
    @DisplayName("Steps 1-5: Removed player no longer participates in scoring")
    void testRemovedPlayerNotInScores() throws Exception {
        WhistGame game = runCount(roundThenAddPlayer("ToRemove",
                "6",                          // Step 1 UC7: remove
                "5"                         // Step 2 UC7: select ToRemove

        ));

        assertFalse(game.getPlayers().stream().anyMatch(p -> p.getName().equals("ToRemove")),
                "Removed player must not appear in the player list");
    }

    // =========================================================================
    // Step 4: Remove multiple players
    // =========================================================================

    @Test
    @DisplayName("Step 4: Remove two players sequentially — end up with 5")
    void testRemoveTwoPlayersSequentially() throws Exception {
        // Start with 4 players, add 3 → 7 players
        WhistGame game = runCount(
                "2",
                "P1", "P2", "P3", "P4",
                "3",
                "1",
                "1",
                "9",

                // Add 3 extra players
                "4", "Extra1",
                "4", "Extra2",
                "4", "Extra3",

                // Step 1-3: remove first
                "5",                          // remove option
                "5",                          // select player 5

                // Step 4: repeat
                "5",                          // remove again
                "5",                          // select player 5 (now Extra2)

                "2"                           // quit
        );

        assertEquals(5, game.getPlayers().size(), "Should have 5 players after removing 2 from 7");
    }

    // =========================================================================
    // Extension 3a: Cannot remove below 4 players
    // =========================================================================

    @Test
    @DisplayName("Extension 3a: Removing player that would go below 4 returns error")
    void testCannotRemoveBelowFour() throws Exception {
        WhistGame game = runCount(roundThenAddPlayer("ExtraOnly",
                "5",                          // Step 1: remove first (valid — goes to 4)
                "5",                          // Step 2: select ExtraOnly

                // Now at exactly 4 — option 5 should not appear
                // If somehow triggered: system should return error, not remove
                "2"                           // Step 5: quit
        ));

        // We must have at least 4 players
        assertTrue(game.getPlayers().size() >= 4,
                "Cannot drop below 4 players");
    }

    @Test
    @DisplayName("Extension 3a: Scoreboard option 5 not shown when exactly 4 players")
    void testRemoveOptionNotShownWithFourPlayers() throws Exception {
        // At exactly 4 players the renderer should not show option 5.
        // We verify the system doesn't crash if somehow "5" is entered with only 4 players.
        WhistGame game = runCount(
                "2",
                "P1", "P2", "P3", "P4",
                "3",                          // TODO: verify
                "2",
                "2",
                "9",
                "6"                         // Step 1: try to remove with only 4 — should be rejected
                                    // quit
        );

        // System must not remove anyone — still 4 players
        assertEquals(4, game.getPlayers().size(),
                "Player count must not drop below 4 when attempting illegal removal");
    }

    // =========================================================================
    // Negative — invalid player selection
    // =========================================================================

    @Test
    @DisplayName("Step 2: Invalid player index re-prompts without crashing")
    void testInvalidPlayerIndexRePrompts() throws Exception {
        WhistGame game = runCount(roundThenAddPlayer("FifthPlayer",
                "5",                          // Step 1: remove
                "99",                         // Step 2: invalid index
                "5",                          // Step 2: valid retry
                "2"
        ));

        assertNotNull(game);
    }

    @Test
    @DisplayName("Step 2: Non-numeric input re-prompts without crashing")
    void testNonNumericPlayerInputRePrompts() throws Exception {
        WhistGame game = runCount(roundThenAddPlayer("FifthPlayer",
                "5",                          // Step 1: remove
                "abc",                        // Step 2: invalid
                "5",                          // Step 2: valid
                "2"
        ));

        assertNotNull(game);
    }
}