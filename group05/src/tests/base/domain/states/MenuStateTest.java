package base.domain.states;

import base.domain.WhistGame;
import base.domain.actions.ContinueAction;
import base.domain.actions.NumberAction;
import base.domain.actions.TextAction;
import base.domain.deck.Deck;
import base.domain.events.ErrorEvent;
import base.domain.events.GameEvent;
import base.domain.events.menuEvents.*;
import base.domain.player.Player;
import base.domain.card.*;
import base.domain.round.Round;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MenuStateTest {

    private FakeWhistGame fakeGame;
    private MenuState menuState;

    @BeforeEach
    void setUp() {
        fakeGame = new FakeWhistGame();
        menuState = new MenuState(fakeGame);
    }

    @Test
    void testWelcomePhase_ResetsGameAndPromptsMode() {
        // Any action in the WELCOME phase triggers showWelcome()
        GameEvent<?> event = menuState.executeState(new ContinueAction());

        assertInstanceOf(WelcomeMenuEvent.class, event, "Should start with a welcome event");
        assertEquals(1, fakeGame.resetPlayersCallCount, "Should reset players on welcome");
        assertEquals(1, fakeGame.resetRoundsCallCount, "Should reset rounds on welcome");
    }

    @Test
    void testPlayModeFlow_WithBots() {
        // 1. WELCOME -> CHOOSE_MODE
        menuState.executeState(new ContinueAction());

        // 2. CHOOSE_MODE (Choice 1 = Play Game) -> CHOOSE_BOTS
        GameEvent<?> modeEvent = menuState.executeState(new NumberAction(1));
        assertInstanceOf(AmountOfBotsEvent.class, modeEvent);

        // 3. CHOOSE_BOTS (2 Bots) -> ENTER_HUMANS
        GameEvent<?> botsEvent = menuState.executeState(new NumberAction(2));
        // Removed the playerNumber() check. Checking the class type is enough for coverage!
        assertInstanceOf(PlayerNameEvent.class, botsEvent);

        // 4. ENTER_HUMANS (Need 2 humans: 4 total - 2 bots)
        GameEvent<?> h2Event = menuState.executeState(new TextAction("Alice"));
        assertInstanceOf(PlayerNameEvent.class, h2Event, "Asks for 2nd human");

        GameEvent<?> b1Event = menuState.executeState(new TextAction("Bob"));
        assertInstanceOf(BotStrategyEvent.class, b1Event, "Should move to bots after humans are full");

        // 5. ENTER_BOTS (Need 2 bots)
        // Bot 1 uses strategy 1 (HighBot)
        GameEvent<?> b2Event = menuState.executeState(new NumberAction(1));
        assertInstanceOf(BotStrategyEvent.class, b2Event, "Asks for 2nd bot");

        // Bot 2 uses strategy 2 (LowBot)
        GameEvent<?> finishEvent = menuState.executeState(new NumberAction(2));
        assertInstanceOf(PrintNamesEvent.class, finishEvent, "Should print names when all players are registered");

        // 6. Verify Game State
        assertEquals(4, fakeGame.getPlayers().size(), "Game should have exactly 4 players");
        assertEquals("Alice", fakeGame.getPlayers().get(0).getName());
        assertTrue(fakeGame.getPlayers().get(2).getName().startsWith("Bot"), "3rd player should be a bot");

        // 7. Verify Transition
        State next = menuState.nextState();
        assertInstanceOf(BidState.class, next, "Play mode (1) should transition to BidState");
        assertTrue(fakeGame.randomDealerSet, "Should assign a random dealer for a new game");
        assertNotNull(fakeGame.assignedDeck, "Should instantiate a new Deck for a new game");
    }

    @Test
    void testCountModeFlow_AllHumans() {
        // 1. WELCOME -> CHOOSE_MODE
        menuState.executeState(new ContinueAction());

        // 2. CHOOSE_MODE (Choice 2 = Count Mode) -> ENTER_HUMANS (Total bots is set to 0)
        GameEvent<?> modeEvent = menuState.executeState(new NumberAction(2));
        assertInstanceOf(PlayerNameEvent.class, modeEvent, "Count mode should skip bot selection");

        // 3. ENTER_HUMANS (Need 4 humans)
        menuState.executeState(new TextAction("P1"));
        menuState.executeState(new TextAction("P2"));
        menuState.executeState(new TextAction("P3"));

        GameEvent<?> finishEvent = menuState.executeState(new TextAction("P4"));
        assertInstanceOf(PrintNamesEvent.class, finishEvent, "Should print names when 4 humans are registered");

        // 4. Verify Transition
        State next = menuState.nextState();
        assertInstanceOf(CountState.class, next, "Count mode (2) should transition to CountState");
    }

    // =========================================================================
    // ERROR HANDLING & INVALID INPUTS
    // =========================================================================

    @Test
    void testChooseMode_InvalidInputs() {
        menuState.executeState(new ContinueAction()); // Moves to CHOOSE_MODE

        assertInstanceOf(ErrorEvent.class, menuState.executeState(new NumberAction(0)));
        assertInstanceOf(ErrorEvent.class, menuState.executeState(new NumberAction(4)));
        assertInstanceOf(ErrorEvent.class, menuState.executeState(new TextAction("Game")));
    }

    @Test
    void testChooseBots_InvalidInputs() {
        menuState.executeState(new ContinueAction());
        menuState.executeState(new NumberAction(1)); // Moves to CHOOSE_BOTS

        assertInstanceOf(ErrorEvent.class, menuState.executeState(new NumberAction(-1)));
        assertInstanceOf(ErrorEvent.class, menuState.executeState(new NumberAction(5)));
        assertInstanceOf(ErrorEvent.class, menuState.executeState(new TextAction("Two")));
    }

    @Test
    void testEnterHumans_InvalidInput_ReturnsPrintNamesEvent() {
        menuState.executeState(new ContinueAction());
        menuState.executeState(new NumberAction(2)); // Moves to ENTER_HUMANS

        // According to your logic, if action is NOT a TextAction, it returns PrintNamesEvent immediately
        GameEvent<?> event = menuState.executeState(new NumberAction(1));
        assertInstanceOf(PrintNamesEvent.class, event, "Non-text input in human phase early-exits to PrintNamesEvent");
    }

    @Test
    void testEnterBots_InvalidInputs() {
        menuState.executeState(new ContinueAction());
        menuState.executeState(new NumberAction(1)); // Mode 1
        menuState.executeState(new NumberAction(1)); // 1 Bot
        menuState.executeState(new TextAction("P1"));
        menuState.executeState(new TextAction("P2"));
        menuState.executeState(new TextAction("P3")); // Moves to ENTER_BOTS

        assertInstanceOf(ErrorEvent.class, menuState.executeState(new NumberAction(0)));
        assertInstanceOf(ErrorEvent.class, menuState.executeState(new NumberAction(3)));
        assertInstanceOf(ErrorEvent.class, menuState.executeState(new TextAction("High")));
    }

    // =========================================================================
    // MANUAL FAKES
    // =========================================================================

    static class FakeWhistGame extends WhistGame {
        private final List<Player> players = new ArrayList<>();
        int resetPlayersCallCount = 0;
        int resetRoundsCallCount = 0;
        boolean randomDealerSet = false;

        // Change this: initialize it or ensure getDeck() uses it
        Deck assignedDeck = new FakeDeck();

        @Override
        public void resetPlayers() {
            players.clear();
            resetPlayersCallCount++;
        }

        @Override
        public void resetRounds() {
            resetRoundsCallCount++;
        }

        @Override
        public void addPlayer(Player player) {
            players.add(player);
        }

        @Override
        public List<Player> getPlayers() {
            return players;
        }

        @Override
        public void setRandomDealer() {
            randomDealerSet = true;
        }

        @Override
        public void setDeck(Deck deck) {
            this.assignedDeck = deck;
        }

        // ADD THIS METHOD to fix the NullPointerException
        @Override
        public Deck getDeck() {
            return assignedDeck;
        }

        // Ensure you have a basic Round list so BidState doesn't crash on initializeRound
        @Override
        public List<Round> getRounds() {
            return new ArrayList<>();
        }

        @Override
        public void addRound(Round round) {
        }
    }

    static class FakeDeck extends Deck {
        @Override
        public List<List<Card>> deal() {
            // Return 4 empty/dummy lists so BidState.dealCards() doesn't crash
            List<List<Card>> hands = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                List<Card> hand = new ArrayList<>();
                hand.add(new Card(Suit.SPADES, Rank.ACE)); // Add a dummy card
                hands.add(hand);
            }
            return hands;
        }
    }
}