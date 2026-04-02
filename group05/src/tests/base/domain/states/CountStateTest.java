package base.domain.states;

import base.domain.WhistGame;
import base.domain.actions.ContinueAction;
import base.domain.actions.NumberAction;
import base.domain.actions.NumberListAction;
import base.domain.actions.TextAction;
import base.domain.events.ErrorEvent;
import base.domain.events.GameEvent;
import base.domain.events.countEvents.*;
import base.domain.events.errorEvents.NumberListErrorEvent;
import base.domain.player.HighBotStrategy;
import base.domain.player.Player;
import base.domain.round.Round;
import base.domain.states.CountState;
import base.domain.states.MenuState;
import base.domain.states.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CountStateTest {

    private FakeWhistGame fakeGame;
    private CountState countState;
    private FakePlayer p1, p2, p3, p4;

    @BeforeEach
    void setUp() {
        p1 = new FakePlayer("Alice");
        p2 = new FakePlayer("Bob");
        p3 = new FakePlayer("Charlie");
        p4 = new FakePlayer("Diana");

        fakeGame = new FakeWhistGame(Arrays.asList(p1, p2, p3, p4));
        countState = new CountState(fakeGame);
    }

    @Test
    void testFullFlow_NormalBid_Solo() {
        // 1. START Phase
        GameEvent<?> startEvent = countState.executeState(new ContinueAction());
        assertInstanceOf(WelcomeCountEvent.class, startEvent, "Should initialize with Welcome event");

        // 2. SELECT_BID Phase (9 = Solo)
        GameEvent<?> bidEvent = countState.executeState(new NumberAction(9));
        assertInstanceOf(GetSuitEvent.class, bidEvent, "Normal bids should prompt for a Trump suit");

        // 3. SELECT_TRUMP Phase (1 = HEARTS)
        GameEvent<?> trumpEvent = countState.executeState(new NumberAction(1));
        assertInstanceOf(PlayersInBidEvent.class, trumpEvent, "Should prompt for players after suit is selected");

        // 4. SELECT_PLAYERS Phase (Player 1)
        GameEvent<?> playersEvent = countState.executeState(new NumberListAction(new ArrayList<>(List.of(1))));
        assertInstanceOf(TrickWonEvent.class, playersEvent, "Normal bids should prompt for tricks won");

        // 5. CALCULATE Phase (Won 6 tricks)
        GameEvent<?> calcEvent = countState.executeState(new NumberAction(6));
        assertInstanceOf(ScoreBoardEvent.class, calcEvent, "Should show scoreboard after calculation");
        assertEquals(1, fakeGame.rounds.size(), "A new round should have been added to the game");

        // 6. PROMPT_NEXT_STATE Phase (Choice 2 = MenuState)
        GameEvent<?> endEvent = countState.executeState(new NumberAction(2));
        assertInstanceOf(EndOfCountStateEvent.class, endEvent);

        // 7. Verify Transition
        State next = countState.nextState();
        assertInstanceOf(MenuState.class, next, "Choosing 2 should transition to MenuState");
    }

    @Test
    void testFullFlow_MiserieBid_MultipleParticipants() {
        countState.executeState(new ContinueAction()); // Move to SELECT_BID

        // 7 = Miserie (Skips SELECT_TRUMP and goes straight to SELECT_PLAYERS)
        GameEvent<?> bidEvent = countState.executeState(new NumberAction(7));
        assertInstanceOf(PlayersInBidEvent.class, bidEvent);

        // P1 and P2 both play Miserie
        GameEvent<?> playersEvent = countState.executeState(new NumberListAction(new ArrayList<>(List.of(1, 2))));
        assertInstanceOf(MiserieWinnerEvent.class, playersEvent, "Miserie bids should prompt for winners, not tricks");

        // P2 successfully wins the Miserie
        GameEvent<?> calcEvent = countState.executeState(new NumberListAction(new ArrayList<>(List.of(2))));
        assertInstanceOf(ScoreBoardEvent.class, calcEvent);

        // Next State Choice 1 = CountState (Another round)
        countState.executeState(new NumberAction(1));
        State next = countState.nextState();
        assertInstanceOf(CountState.class, next, "Choosing 1 should start a new CountState");
    }

    @Test
    void testCalculatePhase_MiserieNoWinners() {
        countState.executeState(new ContinueAction());
        countState.executeState(new NumberAction(7)); // Miserie
        countState.executeState(new NumberListAction(new ArrayList<>(List.of(1, 2)))); // P1, P2 participate

        // Send [-1] meaning NO ONE won the miserie
        GameEvent<?> calcEvent = countState.executeState(new NumberListAction(new ArrayList<>(List.of(-1))));
        assertInstanceOf(ScoreBoardEvent.class, calcEvent, "Should accept [-1] as no winners and calculate safely");
    }

    // =========================================================================
    // ERROR HANDLING & OUT-OF-BOUNDS TESTS
    // =========================================================================

    @Test
    void testSelectBid_InvalidInputs() {
        countState.executeState(new ContinueAction());

        // Out of bounds
        assertInstanceOf(ErrorEvent.class, countState.executeState(new NumberAction(0)));
        assertInstanceOf(ErrorEvent.class, countState.executeState(new NumberAction(11)));

        // Wrong action type
        assertNull(countState.executeState(new TextAction("Hello")), "Should ignore wrong action types");
    }

    @Test
    void testSelectTrump_InvalidInputs() {
        countState.executeState(new ContinueAction());
        countState.executeState(new NumberAction(9)); // Solo

        // Out of bounds suits
        assertInstanceOf(ErrorEvent.class, countState.executeState(new NumberAction(0)));
        assertInstanceOf(ErrorEvent.class, countState.executeState(new NumberAction(5)));
    }

    @Test
    void testSelectPlayers_NormalBidWithMultiplePlayers_ThrowsError() {
        countState.executeState(new ContinueAction());
        countState.executeState(new NumberAction(9)); // Solo
        countState.executeState(new NumberAction(1)); // Hearts

        // Solo only allows 1 player. Sending 2 should trigger an error.
        GameEvent<?> event = countState.executeState(new NumberListAction(new ArrayList<>(List.of(1, 2))));
        assertInstanceOf(NumberListErrorEvent.class, event, "Normal bids cannot have multiple participants");
    }

    @Test
    void testCalculatePhase_NormalBid_InvalidTricks() {
        countState.executeState(new ContinueAction());
        countState.executeState(new NumberAction(9)); // Solo
        countState.executeState(new NumberAction(1)); // Hearts
        countState.executeState(new NumberListAction(new ArrayList<>(List.of(1))));

        // Negative tricks or > 13 tricks
        assertInstanceOf(ErrorEvent.class, countState.executeState(new NumberAction(-1)));
        assertInstanceOf(ErrorEvent.class, countState.executeState(new NumberAction(14)));
    }

    @Test
    void testCalculatePhase_Miserie_InvalidWinner() {
        countState.executeState(new ContinueAction());
        countState.executeState(new NumberAction(7)); // Miserie
        countState.executeState(new NumberListAction(new ArrayList<>(List.of(1)))); // Only P1 participated

        // Tell the state that P2 (Index 2) won the miserie, which is impossible since they didn't play
        GameEvent<?> event = countState.executeState(new NumberListAction(new ArrayList<>(List.of(2))));
        assertInstanceOf(NumberListErrorEvent.class, event, "Cannot declare a winner who did not participate");
    }

    @Test
    void testPromptNextState_InvalidInput() {
        countState.executeState(new ContinueAction());
        countState.executeState(new NumberAction(9));
        countState.executeState(new NumberAction(1));
        countState.executeState(new NumberListAction(new ArrayList<>(List.of(1))));
        countState.executeState(new NumberAction(5)); // Won 5 tricks, moves to PROMPT_NEXT_STATE

        assertInstanceOf(ErrorEvent.class, countState.executeState(new NumberAction(4)), "Only choices 1, 2 and 3 are allowed");
    }

    // =========================================================================
    // MANUAL FAKES
    // =========================================================================

    static class FakePlayer extends Player {
        String testName;
        int testScore = 0;

        public FakePlayer(String name) {
            super(new HighBotStrategy(), name);
            this.testName = name;
        }

        @Override public String getName() { return testName; }
        @Override public Integer getScore() { return testScore; }
    }

    static class FakeWhistGame extends WhistGame {
        List<Player> players;
        List<Round> rounds = new ArrayList<>();

        public FakeWhistGame(List<Player> players) {
            super();
            this.players = players;
        }

        @Override public List<Player> getPlayers() { return players; }
        @Override public void addRound(Round round) { rounds.add(round); }
        @Override public List<Round> getRounds() { return rounds; }
    }

    // =========================================================================
    // MAXIMIZING LINE & BRANCH COVERAGE
    // =========================================================================

    @Test
    void testCreateBidObject_AllNormalBidTypes() {
        // We need to hit cases 1, 2, 3, 4, 5, 6, and 10 in createBidObject.
        // We already tested 9 (Solo).
        int[] normalBidsToTest = {1, 2, 3, 4, 5, 6, 10};

        for (int bidChoice : normalBidsToTest) {
            CountState state = new CountState(fakeGame);
            state.executeState(new ContinueAction()); // START -> SELECT_BID
            state.executeState(new NumberAction(bidChoice)); // SELECT_BID -> SELECT_TRUMP

            // Proposals (1 and 2) technically shouldn't ask for a Trump suit in a real flow,
            // but CountState strictly asks for it unless it's 7 or 8.
            state.executeState(new NumberAction(1)); // SELECT_TRUMP -> SELECT_PLAYERS

            state.executeState(new NumberListAction(new ArrayList<>(List.of(1)))); // -> CALCULATE

            // This final step triggers handleCalculate -> createBidObject
            GameEvent<?> event = state.executeState(new NumberAction(8)); // Won 8 tricks

            assertInstanceOf(ScoreBoardEvent.class, event,
                    "Bid choice " + bidChoice + " failed to calculate correctly.");
        }
    }

    @Test
    void testCreateBidObject_OpenMiserie() {
        // Hit case 8 in createBidObject (Open Miserie)
        CountState state = new CountState(fakeGame);
        state.executeState(new ContinueAction()); // START -> SELECT_BID
        state.executeState(new NumberAction(8)); // 8 = OPEN_MISERIE -> SELECT_PLAYERS
        state.executeState(new NumberListAction(new ArrayList<>(List.of(1)))); // -> CALCULATE

        // P1 wins the open miserie
        GameEvent<?> event = state.executeState(new NumberListAction(new ArrayList<>(List.of(1))));
        assertInstanceOf(ScoreBoardEvent.class, event, "Open Miserie failed to calculate.");
    }

    @Test
    void testWrongActionTypes_ReturnsNullSafely() {
        CountState state = new CountState(fakeGame);
        state.executeState(new ContinueAction()); // Moves to SELECT_BID
        state.executeState(new NumberAction(9));  // Moves to SELECT_TRUMP

        // Context: SELECT_TRUMP expects NumberAction
        assertNull(state.executeState(new TextAction("Hello")),
                "Passing text to SELECT_TRUMP should safely return null");

        state.executeState(new NumberAction(1)); // Moves to SELECT_PLAYERS

        // Context: SELECT_PLAYERS expects NumberListAction
        assertNull(state.executeState(new TextAction("Hello")),
                "Passing text to SELECT_PLAYERS should safely return null");
    }

    @Test
    void testCalculatePhase_Miserie_WrongActionType() {
        CountState state = new CountState(fakeGame);
        state.executeState(new ContinueAction()); // Moves to SELECT_BID
        state.executeState(new NumberAction(7));  // Moves to SELECT_PLAYERS (Miserie)
        state.executeState(new NumberListAction(new ArrayList<>(List.of(1)))); // Moves to CALCULATE

        // Context: CALCULATE for Miserie expects NumberListAction (for winners)
        assertNull(state.executeState(new TextAction("Hello")),
                "Passing text to Miserie CALCULATE should safely return null");
    }
}