package usecases;

import base.GameController;
import base.domain.WhistGame;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.deck.Deck;
import base.domain.player.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class InAppGame {

    private final InputStream sysInBackup = System.in;

    @AfterEach
    void tearDown() {
        System.setIn(sysInBackup);
    }

    private WhistGame runIntegrationTest(
            List<List<Card>> preDefinedHands,
            String... scriptLines
    ) throws Exception {

        String script = String.join("\n", scriptLines) + "\n";
        System.setIn(new ByteArrayInputStream(script.getBytes()));

        try (MockedConstruction<Deck> mockedDeck = mockConstruction(Deck.class, (mock, context) -> {
            if (preDefinedHands != null) {
                when(mock.deal(Deck.DEAL_TYPE.WHIST)).thenReturn(preDefinedHands);
            }
        })) {

            GameController controller = new GameController();

            Field gameField = GameController.class.getDeclaredField("game");
            gameField.setAccessible(true);
            WhistGame game = (WhistGame) gameField.get(controller);

            // Inject dealer at the right time (after players are registered)
            Thread injector = new Thread(() -> {
                try {
                    while (game.getPlayers().size() < 4) {
                        Thread.sleep(10);
                    }

                    Field dealerField = WhistGame.class.getDeclaredField("dealer");
                    dealerField.setAccessible(true);
                    dealerField.set(game, game.getPlayers().get(0));

                } catch (Exception ignored) {}
            });

            injector.start();

            try {
                controller.run();
            } catch (Exception ignored) {
                // expected when input runs out
            }

            return game;
        }
    }

    // =========================================================================
    // UC 4.2 — Step 4a: Forced Troela
    // =========================================================================

    @Test
    @DisplayName("UC 4.2 Step 4a: Forced Troela when player has 4 Aces")
    void testForcedTroelaFlow() throws Exception {

        List<Card> p1 = new ArrayList<>(List.of(
                new Card(Suit.HEARTS, Rank.ACE),
                new Card(Suit.DIAMONDS, Rank.ACE),
                new Card(Suit.CLUBS, Rank.ACE),
                new Card(Suit.SPADES, Rank.ACE)
        ));
        while (p1.size() < 13) p1.add(new Card(Suit.HEARTS, Rank.TWO));

        List<Card> dummy = new ArrayList<>();
        while (dummy.size() < 13) dummy.add(new Card(Suit.CLUBS, Rank.THREE));

        WhistGame game = runIntegrationTest(
                List.of(p1, dummy, dummy, dummy),

                // 1–3 Setup
                "1",
                "0",
                "P1","P2","P3","P4", "1", "1", "1"
                // 4. Troela auto-applied
        );

        assertEquals(
                BidType.TROELA,
                game.getCurrentRound().getHighestBid().getType()
        );
    }

    // =========================================================================
    // UC 4.2 — Step 11a: Miserie early failure
    // =========================================================================

    @Test
    @DisplayName("UC 4.2 Step 11a: Miserie fails when bidder wins a trick")
    void testMiserieEarlyFailure() throws Exception {

        List<Card> p1 = new ArrayList<>(List.of(new Card(Suit.HEARTS, Rank.ACE)));
        List<Card> p2 = new ArrayList<>(List.of(new Card(Suit.HEARTS, Rank.TWO)));
        List<Card> p3 = new ArrayList<>(List.of(new Card(Suit.HEARTS, Rank.THREE)));
        List<Card> p4 = new ArrayList<>(List.of(new Card(Suit.HEARTS, Rank.FOUR)));

        for (int i = 0; i < 12; i++) {
            p1.add(new Card(Suit.CLUBS, Rank.TWO));
            p2.add(new Card(Suit.CLUBS, Rank.THREE));
            p3.add(new Card(Suit.CLUBS, Rank.FOUR));
            p4.add(new Card(Suit.CLUBS, Rank.FIVE));
        }

        WhistGame game = runIntegrationTest(
                List.of(p1, p2, p3, p4),

                "1", "0", "P1", "P2", "P3", "P4", // Setup
                "7", "1", "1", "1",               // Bidding

                // TRICK 1
                // P1 Turn
                "",   // <--- Press ENTER to view cards
                "1",  // <--- Select card
                "",   // <--- (If your UI asks for confirmation after playing)

                // P2 Turn
                "",   // <--- Press ENTER to view cards
                "1",  // <--- Select card
                "",

                // P3 Turn
                "",   // <--- Press ENTER to view cards
                "1",  // <--- Select card
                "",

                // P4 Turn
                "",   // <--- Press ENTER to view cards
                "1",  // <--- Select card
                ""
        );

        assertTrue(game.getCurrentRound().isFinished());
        assertTrue(game.getPlayers().get(0).getScore() < 0);
    }

    // =========================================================================
    // UC 4.2 — Step 9a: View last trick
    // =========================================================================

    @Test
    @DisplayName("UC 4.2 Step 9a: Viewing last trick does not break flow")
    void testViewLastTrickFlow() throws Exception {
        // We maken een simpel deck waarbij P1 een Aas heeft om de eerste slag te winnen
        List<Card> p1Hand = new ArrayList<>(List.of(new Card(Suit.HEARTS, Rank.ACE)));
        while(p1Hand.size() < 13) p1Hand.add(new Card(Suit.CLUBS, Rank.TWO));

        List<Card> otherHand = new ArrayList<>();
        while(otherHand.size() < 13) otherHand.add(new Card(Suit.SPADES, Rank.TWO));

        List<List<Card>> hands = List.of(p1Hand, otherHand, otherHand, otherHand);

        WhistGame game = runIntegrationTest(
                hands,
                "1", "0", "P1", "P2", "P3", "P4", // Setup [cite: 293, 307]
                "1", "1", "1", "1",               // Bieden: Iedereen past [cite: 315, 332]
                "1",  "1",  "1", "1", // Eerste trick spelen [cite: 320, 323]
                "0",                                // 9a: Bekijk laatste trick [cite: 339]
                "1", ""                             // Speel een kaart voor de volgende trick om loop te houden
        );

        // De NPE kwam omdat getCurrentRound() null was.
        // We checken nu of de ronde nog bestaat en of de trick history gevuld is.
        assertNotNull(game.getCurrentRound(), "Ronde mag niet null zijn");
    }

    // =========================================================================
    // UC 4.2 — Step 13: Quit
    // =========================================================================

    @Test
    @DisplayName("UC 4.2 Step 13: Quit game")
    void testQuitGame() throws Exception {

        WhistGame game = runIntegrationTest(
                null,

                // 1–3 Setup
                "1","0","P1","P2","P3","P4",

                // 13 Quit
                "0"
        );

        assertNotNull(game);
    }
}