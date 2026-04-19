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
 * UC 4.6 — Add player.
 *
 * Count menu: "2" / names (no bot/human count asked in count mode)
 * Scoreboard option "5" = add player (count mode always adds human).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC 4.6 — Add player")
class UC6_AddPlayerTest {

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
    // Steps 1-5: Add a single player
    // =========================================================================

    @Test
    @DisplayName("Steps 1-5: Add one player — added with score 0")
    void testAddOnePlayer() throws Exception {
        WhistGame game = runCount(
                "2",                              // Step 1 UC1: count mode
                "P1", "P2", "P3", "P4",          // Step 2 UC1: names
                "3",                              // Step 5: bid
                "2", "1", "10",                   // trump, bidder, tricks
                "5",                              // Step 1 UC6: add player option in scoreboard
                "NewPlayer"                       // Step 2 UC6: name
        );

        List<Player> players = game.getAllPlayers();
        assertTrue(players.size() > 4);
        Player np = players.stream().filter(p -> p.getName().equals("NewPlayer")).findFirst().orElse(null);
        assertNotNull(np);
        assertEquals(0, np.getScore());
    }

    @Test
    @DisplayName("Steps 1-5: New player always starts at score 0 regardless of others")
    void testNewPlayerStartsAtZero() throws Exception {
        WhistGame game = runCount(
                "2",
                "P1", "P2", "P3", "P4",
                "3",                              // TODO: verify
                "1", "2", "13",
                "5",                              // Step 1 UC6: add
                "Newcomer"                        // Step 2 UC6: name
        );

        Player newcomer = game.getAllPlayers().stream()
                .filter(p -> p.getName().equals("Newcomer")).findFirst().orElse(null);
        assertNotNull(newcomer);
        assertEquals(0, newcomer.getScore());
    }

    // =========================================================================
    // Step 4: Add multiple players
    // =========================================================================

    @Test
    @DisplayName("Step 4: Add two players sequentially — both at score 0")
    void testAddTwoPlayersSequentially() throws Exception {
        WhistGame game = runCount(
                "2",
                "P1", "P2", "P3", "P4",
                "3",                              // TODO: verify
                "2", "1", "9",
                "5", "Fifth",                     // Step 1-3 UC6: first add
                "5", "Sixth"                      // Step 4 UC6: second add
        );

        assertEquals(6, game.getAllPlayers().size());
        game.getAllPlayers().stream()
                .filter(p -> p.getName().equals("Fifth") || p.getName().equals("Sixth"))
                .forEach(p -> assertEquals(0, p.getScore()));
    }

    // =========================================================================
    // Negative — blank name rejected
    // =========================================================================

    @Test
    @DisplayName("Step 2: Blank name is rejected and re-prompted")
    void testBlankNameRejected() throws Exception {
        WhistGame game = runCount(
                "2",
                "P1", "P2", "P3", "P4",
                "3",                              // TODO: verify
                "1", "1", "9",
                "5",                              // Step 1 UC6: add
                "",                               // blank — re-prompt
                "ValidName"                       // valid retry
        );

        assertNotNull(game);
        assertTrue(game.getAllPlayers().stream().anyMatch(p -> p.getName().equals("ValidName")));
        assertFalse(game.getAllPlayers().stream().anyMatch(p -> p.getName().isBlank()));
    }
}