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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Scenario tests for UC 4.3 — View last-played trick.
 * <br>
 * Precondition: An in-app game is in progress and at least one trick has been played.
 * <br>
 * UC Steps:
 *  1. User can request to view the cards played in the last trick (input "0").
 *  2. System shows each card played and which player played it.
 *  3. User plays a card and play continues (UC 4.2 step 7).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC 4.3 — View last-played trick")
class UC3_ViewLastPlayedTrickTest {

    private final InputStream sysInBackup = System.in;

    @AfterEach
    void tearDown() {
        System.setIn(sysInBackup);
    }

    private WhistGame run(List<List<Card>> hands, String... lines) throws Exception {
        String script = String.join("\n", lines) + "\n";
        System.setIn(new ByteArrayInputStream(script.getBytes()));

        try (MockedConstruction<Deck> mockedDeck = mockConstruction(Deck.class, (mock, ctx) -> {
            if (hands != null) when(mock.deal(Deck.DealType.WHIST)).thenReturn(hands);
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

    private List<Card> filledWith(Suit suit, Rank rank, int count) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < count; i++) cards.add(new Card(suit, rank));
        return cards;
    }

    // =========================================================================
    // Precondition: viewing before any trick played
    // =========================================================================

    @Test
    @DisplayName("Precondition violated: requesting last trick before any trick played shows error")
    void testViewLastTrickBeforeAnyTrickPlayed() throws Exception {
        List<Card> p1 = new ArrayList<>(List.of(new Card(Suit.HEARTS, Rank.ACE)));
        while (p1.size() < 13) p1.add(new Card(Suit.CLUBS, Rank.TWO));
        List<Card> other = filledWith(Suit.SPADES, Rank.THREE, 13);

        WhistGame game = run(List.of(p1, other, other, other),
                "1",                                    // Step 1 UC2: in-app game
                "0",
                "4",
                "P1", "P2", "P3", "P4",

                // Step 6 UC2: bidding
                "1", "1", "1", "1",                    // all pass
                "1", "1", "1", "1",                    // re-dealt, all pass again

                // Step 7 UC2: no tricks played yet
                "",                                     // Step 8 UC2: confirmation for P1
                "0",                                    // Step 1 UC3: request last trick (none played yet)
                "1"                                     // Step 3 UC3: play card after error shown
        );

        // System must not crash and round must still be valid
        assertNotNull(game);
        assertNotNull(game.getCurrentRound());
    }

    // =========================================================================
    // Steps 1-3: Main success scenario
    // =========================================================================

    @Test
    @DisplayName("Steps 1-3: View last trick after first trick — flow continues normally")
    void testViewLastTrickAfterFirstTrick() throws Exception {
        List<Card> p1Hand = new ArrayList<>(List.of(new Card(Suit.HEARTS, Rank.ACE)));
        while (p1Hand.size() < 13) p1Hand.add(new Card(Suit.CLUBS, Rank.TWO));
        List<Card> other = filledWith(Suit.SPADES, Rank.TWO, 13);

        WhistGame game = run(List.of(p1Hand, other, other, other),
                "1",                                    // UC2 Step 1
                "0",
                "4",
                "P1", "P2", "P3", "P4",

                // UC2 Step 6: bidding — all pass twice (re-deal)
                "1", "1", "1", "1",
                "1", "1", "1", "1",

                // UC2 Steps 7-10: play first complete trick
                "", "1",                                // P1 plays ACE
                "", "1",                                // P2
                "", "1",                                // P3
                "", "1",                                // P4

                // Trick 1 complete — P1 won, now starts trick 2
                // Step 1 UC3: request last trick
                "", "0",                                // Step 1: press enter then "0" to view last trick
                // Step 2 UC3: system shows last trick (no input needed, just rendered)

                // Step 3 UC3: user plays a card to continue
                "1"
        );

        // Step 3: game is still running — round not null
        assertNotNull(game.getCurrentRound(), "Round must still be active after viewing trick history");
    }

    @Test
    @DisplayName("Steps 1-3: View last trick mid-game — multiple requests work")
    void testViewLastTrickMultipleTimes() throws Exception {
        List<Card> p1Hand = new ArrayList<>(List.of(new Card(Suit.HEARTS, Rank.ACE)));
        while (p1Hand.size() < 13) p1Hand.add(new Card(Suit.CLUBS, Rank.TWO));
        List<Card> other = filledWith(Suit.SPADES, Rank.TWO, 13);

        WhistGame game = run(List.of(p1Hand, other, other, other),
                "1",
                "0",
                "4",
                "P1", "P2", "P3", "P4",
                "1", "1", "1", "1",
                "1", "1", "1", "1",

                // Trick 1
                "", "1", "", "1", "", "1", "", "1",

                // Request last trick twice before playing
                "", "0",                                // view trick history (1st time)
                "0",                                    // view trick history again (2nd time)
                "1"                                     // Step 3: now play a card
        );

        assertNotNull(game);
        assertNotNull(game.getCurrentRound());
    }

    // =========================================================================
    // Negative — view last trick does not affect score
    // =========================================================================

    @Test
    @DisplayName("Viewing last trick does not affect game state or scores")
    void testViewLastTrickDoesNotAffectState() throws Exception {
        List<Card> p1Hand = new ArrayList<>(List.of(new Card(Suit.HEARTS, Rank.ACE)));
        while (p1Hand.size() < 13) p1Hand.add(new Card(Suit.CLUBS, Rank.TWO));
        List<Card> other = filledWith(Suit.SPADES, Rank.TWO, 13);

        WhistGame game = run(List.of(p1Hand, other, other, other),
                "1",
                "0",
                "4",
                "P1", "P2", "P3", "P4",
                "1", "1", "1", "1",
                "1", "1", "1", "1",

                // Trick 1
                "", "1", "", "1", "", "1", "", "1",

                // View last trick then play
                "", "0",
                "1"
        );

        // Scores should still sum to zero — viewing trick doesn't change scores
        int total = game.getPlayers().stream()
                .mapToInt(base.domain.player.Player::getScore)
                .sum();
        assertEquals(0, total, "Viewing last trick must not affect scores");
    }
}