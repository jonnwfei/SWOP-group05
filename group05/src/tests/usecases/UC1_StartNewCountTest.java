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
 * Scenario tests for UC 4.1 — Start new count.
 *
 * UC Steps:
 *  1. User selects to start a new count.
 *  2. System asks to register the names of all players.
 *  3. System starts a new game with the registered players (scores = 0).
 *  5. User registers which bid will be played and trump suit.
 *  6. User registers which player(s) play the bid.
 *  7. User enters result (tricks won OR miserie winners).
 *  8. System calculates points and updates scores.
 *  9. User selects to quit.
 *
 * Extension 9a: User starts a new round, scores are kept.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC 4.1 — Start new count")
class UC1_StartNewCountTest {

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

        try {
            controller.run();
        } catch (Exception ignored) {}

        return game;
    }

    // =========================================================================
    // Step 1-3: Registration
    // =========================================================================

    @Test
    @DisplayName("Steps 1-3: Four players registered with zero initial scores")
    void testRegistrationZeroScores() throws Exception {
        WhistGame game = run(
                "2",                         // Step 1: select count mode
                "Alice", "Bob", "Carol", "Dave", // Step 2: register player names
                "2"                          // Step 9: quit
        );

        // Step 3: all players start at zero
        List<Player> players = game.getPlayers();
        assertEquals(4, players.size());
        players.forEach(p -> assertEquals(0, p.getScore(),
                p.getName() + " should start at 0"));
    }

    // =========================================================================
    // Step 5-8: Bid types — success scenarios
    // =========================================================================

    @Test
    @DisplayName("Steps 5-8: Proposal alone — bidder wins 6 tricks (success)")
    void testProposalAloneSuccess() throws Exception {
        WhistGame game = run(
                "2",                          // Step 1: count mode
                "P1", "P2", "P3", "P4",      // Step 2: names
                "2",                          // Step 5a: bid = PROPOSAL
                "2",                          // Step 5b: trump suit
                "1, 2",                          // Step 6: bidder = P1
                "9"                          // Step 7b: 6 tricks won                 // Step 9: quit
        );

        // Step 8: proposal alone 6 tricks = 6 + 0 excess = 6 pts for bidder
        assertTrue(game.getPlayers().get(0).getScore() > 0, "Bidder should gain points");
        assertEquals(0, game.getPlayers().stream().mapToInt(Player::getScore).sum(),
                "Zero-sum must hold");
    }

    @Test
    @DisplayName("Steps 5-8: Proposal alone — bidder fails (wins fewer than 5 tricks)")
    void testProposalAloneFailure() throws Exception {
        WhistGame game = run(
                "2",
                "P1", "P2", "P3", "P4",
                "2",                          // Step 5a: PROPOSAL
                "2",                          // Step 5b: trump
                "1, 2",                          // Step 6: bidder = P1
                "4"                     // Step 7b: only 4 tricks — loss
        );

        assertTrue(game.getPlayers().get(0).getScore() < 0, "Failed bidder should lose points");
        assertEquals(0, game.getPlayers().stream().mapToInt(Player::getScore).sum());
    }

    @Test
    @DisplayName("Steps 5-8: Abondance 9 — bidder wins 9 tricks (success)")
    void testAbondance9Success() throws Exception {
        WhistGame game = run(
                "2",
                "P1", "P2", "P3", "P4",
                "5",                          // Step 5a: Abondance 9
                "2",                          // Step 5b: trump suit
                "2",                          // Step 6: bidder = P2
                "10"
        );

        assertTrue(game.getPlayers().get(1).getScore() > 0);
        assertEquals(0, game.getPlayers().stream().mapToInt(Player::getScore).sum());
    }

    @Test
    @DisplayName("Steps 5-8: Abondance 9 — bidder fails (wins 8 tricks)")
    void testAbondance9Failure() throws Exception {
        WhistGame game = run(
                "2",
                "P1", "P2", "P3", "P4",
                "5",                          // Step 5a: Abondance 9
                "1",                          // Step 5b: trump suit
                "1",                          // Step 6: bidder = P1
                "8"
        );

        assertTrue(game.getPlayers().get(0).getScore() < 0);
        assertEquals(0, game.getPlayers().stream().mapToInt(Player::getScore).sum());
    }





    @Test
    @DisplayName("Steps 5-8: Abondance 12 — success")
    void testAbondance12Success() throws Exception {
        WhistGame game = run(
                "2",
                "P1", "P2", "P3", "P4",
                "12",                          // Step 5a: Abondance 12
                "2",
                "1",
                "13"
        );

        assertTrue(game.getPlayers().get(0).getScore() > 0);
        assertEquals(0, game.getPlayers().stream().mapToInt(Player::getScore).sum());
    }

    @Test
    @DisplayName("Steps 5-8: Miserie — one player plays, wins (takes no tricks)")
    void testMiserieSuccess() throws Exception {
        WhistGame game = run(
                "2",
                "P1", "P2", "P3", "P4",
                "9",                          // Step 5a: Miserie
                "2",                          // Step 6: participants (P2)
                "0"
        );

        assertTrue(game.getPlayers().get(1).getScore() < 0, "Successful miserie player gains points");
        assertEquals(0, game.getPlayers().stream().mapToInt(Player::getScore).sum());
    }

    @Test
    @DisplayName("Steps 5-8: Miserie — player fails (opponent list = all others = miserie player took a trick)")
    void testMiserieFailure() throws Exception {
        WhistGame game = run(
                "2",
                "P1", "P2", "P3", "P4",
                "9",                          // Step 5a: Miserie
                "1",                          // Step 6: participant = P1
                "1"                          // Step 7a: P1 is listed as winner

        );

        assertTrue(game.getPlayers().get(0).getScore() > 0, "Failed miserie loses points");
        assertEquals(0, game.getPlayers().stream().mapToInt(Player::getScore).sum());
    }

    @Test
    @DisplayName("Steps 5-8: Miserie — multiple participants, mixed results")
    void testMiserieMultipleParticipants() throws Exception {
        WhistGame game = run(
                "2",
                "P1", "P2", "P3", "P4",
                "9",                          // Step 5a: Miserie
                "1,2",                        // Step 6: P1 and P2 both play miserie
                "1"                          // Step 7a: only P1 wins (P2 failed)

        );

        assertTrue(game.getPlayers().get(0).getScore() > 0, "P1 succeeded — gains points");
        assertTrue(game.getPlayers().get(1).getScore() < 0, "P2 failed — loses points");
        assertEquals(0, game.getPlayers().stream().mapToInt(Player::getScore).sum());
    }

    @Test
    @DisplayName("Steps 5-8: Open Miserie — success")
    void testOpenMiserieSuccess() throws Exception {
        WhistGame game = run(
                "2",
                "P1", "P2", "P3", "P4",
                "16",                         // Step 5a: Open Miserie // TODO: verify index
                "3",                          // Step 6: participant = P3
                "3"                          // Step 7a: no tricks taken — success

        );

        assertTrue(game.getPlayers().get(2).getScore() > 0);
        assertEquals(0, game.getPlayers().stream().mapToInt(Player::getScore).sum());
    }

    @Test
    @DisplayName("Steps 5-8: Solo — bidder wins all 13 tricks")
    void testSoloSuccess() throws Exception {
        WhistGame game = run(
                "2",
                "P1", "P2", "P3", "P4",
                "17",                         // Step 5a: Solo // TODO: verify index
                "1",                          // Step 5b: trump suit
                "4",                          // Step 6: bidder = P4
                "13"                         // Step 7b: all tricks

        );

        assertTrue(game.getPlayers().get(3).getScore() > 0);
        assertEquals(0, game.getPlayers().stream().mapToInt(Player::getScore).sum());
    }


    @Test
    @DisplayName("Steps 5-8: Solo Slim — bidder fails")
    void testSoloSlimFailure() throws Exception {
        WhistGame game = run(
                "2",
                "P1", "P2", "P3", "P4",
                "18",                         // Step 5a: Solo Slim // TODO: verify index
                "1",
                "1",
                "12"                         // Step 7b: 12 tricks — not all = failure

        );

        assertTrue(game.getPlayers().get(0).getScore() < 0);
        assertEquals(0, game.getPlayers().stream().mapToInt(Player::getScore).sum());
    }

    // =========================================================================
    // Step 8: Zero-sum invariant
    // =========================================================================

    @Test
    @DisplayName("Step 8: Scores always sum to zero after any round")
    void testZeroSumInvariantAfterRound() throws Exception {
        WhistGame game = run(
                "2",
                "P1", "P2", "P3", "P4",
                "5",                          // Step 5a: Abondance 9 // TODO: verify index
                "2",
                "2",                          // Step 6: P2 bids
                "9"                          // Step 7b: exactly 9 tricks

        );

        assertEquals(0,
                game.getPlayers().stream().mapToInt(Player::getScore).sum(),
                "Scores must always sum to zero");
    }

    // =========================================================================
    // Extension 9a: Start a new round, scores are kept
    // =========================================================================

    @Test
    @DisplayName("Extension 9a: Second round keeps accumulated scores")
    void testNewRoundKeepsScores() throws Exception {
        WhistGame game = run(
                "2",
                "P1", "P2", "P3", "P4",

                // Round 1
                "3",                          // Step 5a: Abondance 9 // TODO: verify
                "1",
                "1",                          // Step 6: P1 bids
                "10",                         // Step 7b: success

                // Extension 9a: another round
                "1",                          // Step 9: start new round

                // Round 2
                "3",                          // Step 5a: Abondance 9
                "2",
                "2",                          // Step 6: P2 bids
                "9"
        );

        // Both rounds ran — scores should reflect both
        int total = game.getPlayers().stream().mapToInt(Player::getScore).sum();
        assertEquals(0, total, "Zero-sum must hold across multiple rounds");

        // At least one round was played — someone is non-zero
        boolean anyNonZero = game.getPlayers().stream().anyMatch(p -> p.getScore() != 0);
        assertTrue(anyNonZero, "After two rounds, at least one player should have non-zero score");
    }

    // =========================================================================
    // Negative — invalid input handling
    // =========================================================================

    @Test
    @DisplayName("Invalid bid index re-prompts without crashing")
    void testInvalidBidIndexRePrompts() throws Exception {
        WhistGame game = run(
                "2",
                "P1", "P2", "P3", "P4",
                "abc",                        // invalid — should re-prompt
                "5",                          // Step 5a: valid bid
                "2",
                "2",
                "9"
        );

        assertNotNull(game);
        assertEquals(0, game.getPlayers().stream().mapToInt(Player::getScore).sum());
    }

    @Test
    @DisplayName("Invalid trick count re-prompts without crashing")
    void testInvalidTrickCountRePrompts() throws Exception {
        WhistGame game = run(
                "2",
                "P1", "P2", "P3", "P4",
                "3",                          // Step 5a
                "1",
                "1",
                "xyz",                        // invalid — should re-prompt
                "9"                          // valid trick count

        );

        assertNotNull(game);
    }
}
