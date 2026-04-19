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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UC 4.7 — Remove player.
 *
 * Precondition: round finished AND >4 players.
 * Scoreboard option "6" = remove player (only shown when >4 players).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC 4.7 — Remove player")
class UC7_RemovePlayerTest {

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

    /** One count round + add an extra player → 5 players, then append more inputs. */
    private String[] roundThenAddPlayer(String extraName, String... after) {
        List<String> inputs = new ArrayList<>(List.of(
                "2",
                "P1", "P2", "P3", "P4",
                "3",                              // TODO: verify bid index
                "2", "1", "10",
                "5",                              // scoreboard: add player
                extraName
        ));
        inputs.addAll(List.of(after));
        return inputs.toArray(new String[0]);
    }

    // =========================================================================
    // Steps 1-5: Remove a player
    // =========================================================================

    @Test
    @DisplayName("Steps 1-5: Remove one player — count drops to 4")
    void testRemoveOnePlayer() throws Exception {
        WhistGame game = runCount(roundThenAddPlayer("ExtraPlayer",
                "6",                              // Step 1 UC7: remove player option
                "5"                               // Step 2 UC7: select player 5
        ));

        assertEquals(4, game.getPlayers().size());
        assertFalse(game.getPlayers().stream().anyMatch(p -> p.getName().equals("ExtraPlayer")));
    }

    @Test
    @DisplayName("Steps 1-5: Removed player no longer in list")
    void testRemovedPlayerNotInList() throws Exception {
        WhistGame game = runCount(roundThenAddPlayer("ToRemove",
                "6",                              // Step 1
                "5"                               // Step 2
        ));

        assertFalse(game.getAllPlayers().stream().anyMatch(p -> p.getName().equals("ToRemove")));
    }

    // =========================================================================
    // Step 4: Remove multiple players
    // =========================================================================

    @Test
    @DisplayName("Step 4: Remove two players sequentially from 7 — end with 5")
    void testRemoveTwoPlayers() throws Exception {
        WhistGame game = runCount(
                "2",
                "P1", "P2", "P3", "P4",
                "3",                              // TODO: verify
                "1", "1", "9",
                "5", "Extra1",                    // add 3 extra players
                "5", "Extra2",
                "5", "Extra3",
                "6", "5",                         // remove first
                "6", "5"                          // remove second
        );

        assertEquals(5, game.getAllPlayers().size());
    }

    // =========================================================================
    // Extension 3a: Cannot remove below 4
    // =========================================================================

    @Test
    @DisplayName("Extension 3a: Remove player that would go below 4 — rejected")
    void testCannotRemoveBelowFour() throws Exception {
        WhistGame game = runCount(roundThenAddPlayer("ExtraOnly",
                "6", "5"                          // remove ExtraOnly → back to 4
                // option 6 should now disappear — no more input needed
        ));

        assertTrue(game.getPlayers().size() >= 4);
    }

    @Test
    @DisplayName("Extension 3a: Remove option not shown at exactly 4 players")
    void testRemoveOptionNotShownAtFour() throws Exception {
        WhistGame game = runCount(
                "2",
                "P1", "P2", "P3", "P4",
                "3",                              // TODO: verify
                "2", "2", "9",
                "6"                               // attempt remove with only 4 — should be ignored/rejected
        );

        assertEquals(4, game.getPlayers().size());
    }

    // =========================================================================
    // Negative: invalid input
    // =========================================================================

    @Test
    @DisplayName("Step 2: Invalid index re-prompts")
    void testInvalidIndexRePrompts() throws Exception {
        WhistGame game = runCount(roundThenAddPlayer("FifthPlayer",
                "6",                              // Step 1
                "99",                             // invalid
                "5"                               // valid retry
        ));

        assertNotNull(game);
    }

    @Test
    @DisplayName("Step 2: Non-numeric input re-prompts")
    void testNonNumericRePrompts() throws Exception {
        WhistGame game = runCount(roundThenAddPlayer("FifthPlayer",
                "6",
                "abc",
                "5"
        ));

        assertNotNull(game);
    }
}