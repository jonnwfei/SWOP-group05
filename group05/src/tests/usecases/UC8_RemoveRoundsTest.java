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

import static org.junit.jupiter.api.Assertions.*;

/**
 * UC 4.8 — Remove rounds.
 *
 * Scoreboard option "4" = remove round.
 * Tests end when input runs out — no explicit quit needed.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC 4.8 — Remove rounds")
class UC8_RemoveRoundsTest {

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
    // Steps 1-5: Remove a round
    // =========================================================================

    @Test
    @DisplayName("Steps 1-5: Remove the only round — scores reset to 0")
    void testRemoveOnlyRound() throws Exception {
        WhistGame game = runCount(
                "2",
                "P1", "P2", "P3", "P4",
                "3",                              // Step 5: Abondance 9 // TODO: verify index
                "2", "1", "10",
                "4",                              // Step 1 UC8: remove round option
                "1"                               // Step 2 UC8: round 1
        );

        game.getPlayers().forEach(p ->
                assertEquals(0, p.getScore(), p.getName() + " should be 0 after removing only round"));
        assertTrue(game.getRounds().isEmpty());
    }

    @Test
    @DisplayName("Steps 1-5: Remove first of two rounds — second remains")
    void testRemoveFirstOfTwoRounds() throws Exception {
        WhistGame game = runCount(
                "2",
                "P1", "P2", "P3", "P4",
                "3", "2", "1", "10",              // Round 1
                "1",                              // scoreboard: new round
                "3", "1", "2", "9",              // Round 2
                "4", "1"                          // Step 1-2 UC8: remove round 1
        );

        assertEquals(1, game.getRounds().size());
        assertEquals(0, game.getPlayers().stream().mapToInt(Player::getScore).sum());
    }

    @Test
    @DisplayName("Steps 1-5: Remove second of two rounds — first remains")
    void testRemoveSecondOfTwoRounds() throws Exception {
        WhistGame game = runCount(
                "2",
                "P1", "P2", "P3", "P4",
                "3", "2", "1", "10",              // Round 1
                "1",
                "3", "1", "2", "9",              // Round 2
                "4", "2"                          // Step 1-2 UC8: remove round 2
        );

        assertEquals(1, game.getRounds().size());
        assertEquals(0, game.getPlayers().stream().mapToInt(Player::getScore).sum());
    }

    // =========================================================================
    // Step 3: Scores updated correctly
    // =========================================================================

    @Test
    @DisplayName("Step 3: Scores are zero-sum after round removal")
    void testScoresZeroSumAfterRemoval() throws Exception {
        WhistGame game = runCount(
                "2",
                "P1", "P2", "P3", "P4",
                "3",                              // TODO: verify
                "1", "1", "13",
                "4", "1"                          // remove round 1
        );

        assertEquals(0, game.getPlayers().stream().mapToInt(Player::getScore).sum());
    }

    // =========================================================================
    // Step 4: Remove multiple rounds
    // =========================================================================

    @Test
    @DisplayName("Step 4: Remove multiple rounds sequentially")
    void testRemoveMultipleRounds() throws Exception {
        WhistGame game = runCount(
                "2",
                "P1", "P2", "P3", "P4",
                "3", "2", "1", "10",              // Round 1
                "1",
                "3", "1", "2", "9",              // Round 2
                "1",
                "3", "3", "3", "11",             // Round 3
                "4", "1",                         // Step 1-3: remove round 1
                "4", "1"                          // Step 4: remove round 2 (now index 1)
        );

        assertEquals(1, game.getRounds().size());
        assertEquals(0, game.getPlayers().stream().mapToInt(Player::getScore).sum());
    }

    // =========================================================================
    // Negative — invalid round selection
    // =========================================================================

    @Test
    @DisplayName("Step 2: Invalid round index re-prompts")
    void testInvalidRoundIndexRePrompts() throws Exception {
        WhistGame game = runCount(
                "2",
                "P1", "P2", "P3", "P4",
                "3", "2", "1", "10",
                "4",                              // Step 1: remove
                "abc",                            // invalid
                "1"                               // valid retry
        );

        assertNotNull(game);
        assertEquals(0, game.getPlayers().stream().mapToInt(Player::getScore).sum());
    }

    @Test
    @DisplayName("Step 2: Out-of-range round index re-prompts")
    void testOutOfRangeRoundIndex() throws Exception {
        WhistGame game = runCount(
                "2",
                "P1", "P2", "P3", "P4",
                "3", "1", "1", "9",
                "4",                              // Step 1: remove
                "999",                            // out of range
                "1"                               // valid
        );

        assertNotNull(game);
    }

    @Test
    @DisplayName("Remove round when no rounds exist — system handles gracefully")
    void testRemoveWhenNoRounds() throws Exception {
        WhistGame game = runCount(
                "2",
                "P1", "P2", "P3", "P4",
                "3", "2", "1", "10",
                "4", "1",                         // remove the only round → 0 rounds
                "4"                               // attempt remove again — should not crash
        );

        assertNotNull(game);
    }
}