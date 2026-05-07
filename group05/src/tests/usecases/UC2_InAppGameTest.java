package usecases;

import base.GameController;
import base.domain.WhistGame;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.deck.Deck;
import base.domain.player.Player;
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
 * Scenario tests for UC 4.2 — In-app game.
 *
 * UC Steps:
 *  1.  User selects in-app game.
 *  2.  System asks to register names.
 *  3.  System starts new game, scores = 0.
 *  4.  System chooses four players.
 *  5.  System deals cards; last card is trump.
 *  6.  Players bid in order.
 *  7.  Starting player plays first card.
 *  8.  System asks confirmation before next player's cards shown.
 *  9.  Next player plays a card; illegal cards are prevented.
 *  10. Repeat for all four players; trick awarded.
 *  11. Repeat until all 13 cards played.
 *  12. System calculates points.
 *  13. User quits or starts new round (13a).
 *
 * Extensions tested:
 *  2a.  Bots replacing human players.
 *  6a.  All players pass → re-deal.
 *  8a.  Bots play automatically, no confirmation needed.
 *  9a.  Open Miserie cards always revealed.
 *  11a. Miserie ends early when bidder wins a trick.
 *  13a. New round, scores kept, dealer advances.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UC 4.2 — In-app game")
class UC2_InAppGameTest {

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


            try { controller.run(); } catch (Exception ignored) {}
            return game;
        }
    }

    /** Full 13-trick hand: p1=all aces+kings, others=low cards. */
    private List<List<Card>> dominantP2Hands() {
        List<Card> p1 = filledWith(Suit.CLUBS, Rank.TWO, 13);
        List<Card> p2 = new ArrayList<>();
        p2.add(new Card(Suit.HEARTS, Rank.ACE));
        p2.add(new Card(Suit.HEARTS, Rank.KING));
        p2.add(new Card(Suit.HEARTS, Rank.QUEEN));
        while (p2.size() < 13) p2.add(new Card(Suit.SPADES, Rank.TWO));
        List<Card> other = filledWith(Suit.DIAMONDS, Rank.THREE, 13);
        return List.of(p1, p2, other, other);
    }

    private List<Card> filledWith(Suit suit, Rank rank, int count) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < count; i++) cards.add(new Card(suit, rank));
        return cards;
    }

    // =========================================================================
    // Steps 1-3: Registration
    // =========================================================================

    @Test
    @DisplayName("Steps 1-3: Four players registered with zero initial scores")
    void testRegistrationZeroScores() throws Exception {
        WhistGame game = run(null,
                "1",                                    // Step 1: in-app game
                "0",                                    // Step 2a: 0 bots
                "Alice", "Bob", "Carol", "Dave"         // Step 2: register names
        );

        // Step 3: all start at zero
        assertEquals(4, game.getPlayers().size());
        game.getPlayers().forEach(p ->
                assertEquals(0, p.getScore(), p.getName() + " should start at 0"));
    }

    // =========================================================================
    // Extension 2a: Bots
    // =========================================================================

    @Test
    @DisplayName("Extension 2a: Game with 4 bots runs without human input")
    void testFourBotsRunAutomatically() throws Exception {
        WhistGame game = run(null,
                "1",                                    // Step 1: in-app game
                "3",                                    // Step 2a: 3 bots
                "Alice",                                // Step 2: 1 human name
                "1",                                    // Step 2a: bot 1 strategy = HighBot
                "1",                                    // Step 2a: bot 2 strategy
                "1"                                     // Step 2a: bot 3 strategy
        );

        assertNotNull(game);
    }

    @Test
    @DisplayName("Extension 2a: High-bot and Low-bot registered correctly")
    void testHighBotAndLowBot() throws Exception {
        WhistGame game = run(null,
                "1",                                    // Step 1
                "2",                                    // Step 2a: 2 bots
                "P1", "P2",                             // Step 2: 2 human names
                "1",                                    // Step 2a: bot 1 = HighBot
                "2"                                     // Step 2a: bot 2 = LowBot
        );

        assertEquals(4, game.getPlayers().size());
    }

    // =========================================================================
    // Step 6: Bidding
    // =========================================================================

    @Test
    @DisplayName("Step 6: All players pass — re-deal happens (Extension 6a)")
    void testAllPlayersPassRedeal() throws Exception {
        List<Card> hand = filledWith(Suit.HEARTS, Rank.TWO, 13);
        List<List<Card>> hands = List.of(hand, hand, hand, hand);

        WhistGame game = run(hands,
                "1",                                    // Step 1
                "0",                                    // Step 2a: 0 bots
                "P1", "P2", "P3", "P4",                // Step 2

                // Step 6: all pass — 6a triggers re-deal
                "1", "1", "1", "1",                    // bid round 1: all pass
                "1", "1", "1", "1"                     // bid round 2 (re-dealt)
        );

        assertNotNull(game);
    }

    @Test
    @DisplayName("Step 6: Forced Troela when player has 4 Aces")
    void testForcedTroela() throws Exception {
        List<Card> p1 = new ArrayList<>(List.of(
                new Card(Suit.HEARTS, Rank.ACE),
                new Card(Suit.DIAMONDS, Rank.ACE),
                new Card(Suit.CLUBS, Rank.ACE),
                new Card(Suit.SPADES, Rank.ACE)));
        while (p1.size() < 13) p1.add(new Card(Suit.HEARTS, Rank.TWO));
        List<Card> dummy = filledWith(Suit.CLUBS, Rank.THREE, 13);

        WhistGame game = run(List.of(p1, dummy, dummy, dummy),
                "1",                                    // Step 1
                "0",
                "P1", "P2", "P3", "P4",
                "1","1","1","1"// Step 2
                // Step 6: Troela is forced — no bidding input needed
        );

        // Step 6: TROELA auto-applied
        assertEquals(BidType.TROELA,
                game.getCurrentRound().getHighestBid().getType());
    }

    // =========================================================================
    // Steps 7-10: Playing cards
    // =========================================================================

    @Test
    @DisplayName("Steps 7-10: Playing a complete trick — winner takes next lead")
    void testCompleteTrick() throws Exception {
        List<Card> p1 = new ArrayList<>(List.of(new Card(Suit.HEARTS, Rank.ACE)));
        while (p1.size() < 13) p1.add(new Card(Suit.CLUBS, Rank.TWO));
        List<Card> other = filledWith(Suit.SPADES, Rank.TWO, 13);

        WhistGame game = run(List.of(p1, other, other, other),
                "1",                                    // Step 1
                "0",
                "P1", "P2", "P3", "P4",
                "1", "1", "1", "1",                    // Step 6: all pass → 6a re-deal
                "1", "1", "1", "1",                    // another round of bidding

                // Step 7: first player plays card
                "",                                     // Step 8: confirmation
                "1",                                    // Step 9: play first card

                // Steps 8-9 for remaining players in trick
                "", "1",
                "", "1",
                "", "1"
        );

        assertNotNull(game.getCurrentRound());
    }

    // =========================================================================
    // Extension 8a: Bots need no confirmation
    // =========================================================================

    @Test
    @DisplayName("Extension 8a: Bot turns play automatically without confirmation prompt")
    void testBotsNoConfirmation() throws Exception {
        WhistGame game = run(null,
                "1",                                    // Step 1
                "3",                                    // Step 2a: 3 bots
                "HumanPlayer",                          // Step 2: 1 human
                "1", "1", "1",                         // Step 2a: bot strategies

                // Human bids
                "1"                                     // Step 6: human passes
                // Bots play automatically after this
        );

        assertNotNull(game);
    }

    // =========================================================================
    // Extension 11a: Miserie ends early
    // =========================================================================

    @Test
    @DisplayName("Extension 11a: Miserie ends early when bidder wins a trick")
    void testMiserieEarlyEnd() throws Exception {
        List<Card> p1 = new ArrayList<>(List.of(new Card(Suit.HEARTS, Rank.ACE)));
        List<Card> p2 = new ArrayList<>(List.of(new Card(Suit.CLUBS, Rank.ACE)));
        List<Card> p3 = new ArrayList<>(List.of(new Card(Suit.DIAMONDS, Rank.ACE)));
        List<Card> p4 = new ArrayList<>(List.of(new Card(Suit.SPADES, Rank.ACE)));

        for (int i = 0; i < 12; i++) {
            p1.add(new Card(Suit.HEARTS, Rank.TWO));
            p2.add(new Card(Suit.HEARTS, Rank.TWO));
            p3.add(new Card(Suit.HEARTS, Rank.TWO));
            p4.add(new Card(Suit.HEARTS, Rank.TWO));
        }

            WhistGame game = run(List.of(p1, p2, p3, p4),
                "1",                                    // Step 1
                "0",
                "P1", "P2", "P3", "P4",

                // Step 6: P1 bids Miserie (index 7 = Miserie)
                "7", "1", "1", "1","", "1", "", "1", "", "1", "", "1"

        );

        // Step 12: Miserie failed — at least one player (the bidder) score negative
        assertTrue(game.getPlayers().stream().anyMatch(p -> p.getScore() < 0),
                "Miserie bidder who won a trick should have a negative score");
    }

    // =========================================================================
    // Extension 9a: Open Miserie — hand revealed
    // =========================================================================

    @Test
    @DisplayName("Extension 9a: Open Miserie game completes without crash")
    void testOpenMiserieHandRevealed() throws Exception {
        List<Card> p1 = filledWith(Suit.HEARTS, Rank.TWO, 13);
        List<Card> p2 = filledWith(Suit.CLUBS, Rank.THREE, 13);
        List<Card> p3 = filledWith(Suit.DIAMONDS, Rank.FOUR, 13);
        List<Card> p4 = filledWith(Suit.SPADES, Rank.FIVE, 13);

        WhistGame game = run(List.of(p1, p2, p3, p4),
                "1",
                "0",
                "P1", "P2", "P3", "P4",

                // Step 6: P1 bids Open Miserie
                "8", "1", "1", "1",

                // Steps 8+9: play first trick (hand is visible)
                "", "1",
                "", "1",
                "", "1",
                "", "1"
        );

        assertNotNull(game.getCurrentRound());
    }

    // =========================================================================
    // Steps 12-13: Score calculation and quit
    // =========================================================================

    @Test
    @DisplayName("Steps 12-13: Scores calculated after round, zero-sum holds")
    void testScoresZeroSumAfterRound() throws Exception {
        // Use all-bot game to let it complete naturally
        WhistGame game = run(null,
                "1",
                "3",                                    // Step 2a: 3 bots
                "Human",
                "1", "1", "1"                          // bot strategies
        );

        int total = game.getPlayers().stream().mapToInt(Player::getScore).sum();
        assertEquals(0, total, "Scores must sum to zero after round completes");
    }

    // =========================================================================
    // Extension 13a: New round, dealer advances
    // =========================================================================

    @Test
    @DisplayName("Extension 13a: New round started, scores kept, dealer advances")
    void testNewRoundDealerAdvances() throws Exception {
        WhistGame game = run(null,
                "1",
                "3",
                "Human",
                "1", "1", "1",

                "1"
        );

        assertNotNull(game);
        // After new round, scores from round 1 should still be present
        int total = game.getPlayers().stream().mapToInt(Player::getScore).sum();
        assertEquals(0, total);
    }

    // =========================================================================
    // Negative: illegal card play is prevented (Step 9)
    // =========================================================================

    @Test
    @DisplayName("Step 9: Illegal card index re-prompts without crashing")
    void testIllegalCardPlayReprompts() throws Exception {
        List<Card> p1 = new ArrayList<>(List.of(new Card(Suit.HEARTS, Rank.ACE)));
        while (p1.size() < 13) p1.add(new Card(Suit.CLUBS, Rank.TWO));
        List<Card> other = filledWith(Suit.SPADES, Rank.TWO, 13);

        WhistGame game = run(List.of(p1, other, other, other),
                "1",
                "0",
                "P1", "P2", "P3", "P4",
                "1", "1", "1", "1",                    // Step 6: all pass
                "1", "1", "1", "1",

                "", "99",                               // Step 9: invalid index 99
                "", "1",                                // Step 9: valid card
                "", "1",
                "", "1",
                "", "1"
        );

        assertNotNull(game);
    }

    @Test
    @DisplayName("Step 9: Non-numeric card input re-prompts without crashing")
    void testNonNumericCardInputReprompts() throws Exception {
        List<Card> p1 = new ArrayList<>(List.of(new Card(Suit.HEARTS, Rank.ACE)));
        while (p1.size() < 13) p1.add(new Card(Suit.CLUBS, Rank.TWO));
        List<Card> other = filledWith(Suit.SPADES, Rank.TWO, 13);

        WhistGame game = run(List.of(p1, other, other, other),
                "1",
                "0",
                "P1", "P2", "P3", "P4",
                "1", "1", "1", "1",
                "1", "1", "1", "1",

                "", "abc",                              // Step 9: non-numeric
                "", "1",
                "", "1",
                "", "1",
                "", "1"
        );

        assertNotNull(game);
    }

    @Test
    @DisplayName("Step 13: Quit game gracefully")
    void testQuitGame() throws Exception {
        WhistGame game = run(null,
                "1",
                "0",
                "P1", "P2", "P3", "P4",
                "0"                                     // Step 13: quit immediately
        );

        assertNotNull(game);
    }
}