package base.domain.states;

import base.domain.WhistGame;
import base.domain.actions.ContinueAction;
import base.domain.actions.NumberAction;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.deck.Deck;
import base.domain.events.ErrorEvent;
import base.domain.events.GameEvent;
import base.domain.events.countEvents.ScoreBoardEvent;
import base.domain.events.playevents.ScoreBoardCompleteEvent;
import base.domain.strategy.HumanStrategy;
import base.domain.player.Player;
import base.domain.round.Round;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScoreBoardStateTest {

    private FakeWhistGame fakeGame;
    private ScoreBoardState scoreBoardState;
    private Player p1, p2, p3, p4;

    @BeforeEach
    void setUp() {
        // Using real Player objects to ensure standard behavior
        p1 = new Player(new HumanStrategy(), "Alice");
        p2 = new Player(new HumanStrategy(), "Bob");
        p3 = new Player(new HumanStrategy(), "Charlie");
        p4 = new Player(new HumanStrategy(), "Diana");

        // Give players some scores to display
        p1.updateScore(15);
        p2.updateScore(-5);
        p3.updateScore(10);
        p4.updateScore(-20);

        fakeGame = new FakeWhistGame(Arrays.asList(p1, p2, p3, p4));
        scoreBoardState = new ScoreBoardState(fakeGame);
    }

    @Test
    void testInitialDisplay_ReturnsScoreBoardEvent() {
        // Any non-NumberAction triggers the initial view
        GameEvent<?> event = scoreBoardState.executeState(new ContinueAction());

        assertInstanceOf(ScoreBoardEvent.class, event);
        ScoreBoardEvent sbEvent = (ScoreBoardEvent) event;

        // Verify names and scores were extracted correctly
        assertEquals(4, sbEvent.playerNames().size());
        assertEquals("Alice", sbEvent.playerNames().get(0));
        assertEquals(15, sbEvent.scores().get(0));
        assertEquals(-5, sbEvent.scores().get(1));

        // Verify state stays here if no choice is made
        assertEquals(scoreBoardState, scoreBoardState.nextState(),
                "State should return itself if the user hasn't made a valid choice yet");
    }

    @Test
    void testChoiceRestart_AdvancesDealerAndGoesToBidState() {
        // 1. User chooses 1 (Restart)
        GameEvent<?> event = scoreBoardState.executeState(new NumberAction(1));

        assertInstanceOf(ScoreBoardCompleteEvent.class, event);

        // 2. Check State Transition
        State next = scoreBoardState.nextState();
        assertInstanceOf(BidState.class, next, "Choice 1 should transition to BidState");
        assertTrue(fakeGame.dealerAdvanced, "advanceDealer() must be called before starting a new round");
    }

    @Test
    void testChoiceQuit_GoesToMenuState() {
        // 1. User chooses 2 (Quit)
        GameEvent<?> event = scoreBoardState.executeState(new NumberAction(2));

        assertInstanceOf(ScoreBoardCompleteEvent.class, event);

        // 2. Check State Transition
        State next = scoreBoardState.nextState();
        assertInstanceOf(MenuState.class, next, "Choice 2 should transition to MenuState");
    }

    @Test
    void testInvalidChoice_ReturnsErrorEvent() {
        // User inputs 3 (out of bounds)
        GameEvent<?> eventOver = scoreBoardState.executeState(new NumberAction(3));
        assertInstanceOf(ErrorEvent.class, eventOver);

        // User inputs 0 (out of bounds)
        GameEvent<?> eventUnder = scoreBoardState.executeState(new NumberAction(0));
        assertInstanceOf(ErrorEvent.class, eventUnder);

        // Choice should still be 0 (undecided), staying in ScoreBoardState
        assertEquals(scoreBoardState, scoreBoardState.nextState());
    }

    // =========================================================================
    // BULLETPROOF MANUAL FAKES
    // =========================================================================

    static class FakeWhistGame extends WhistGame {
        private final List<Player> players;
        private final FakeDeck fakeDeck = new FakeDeck();
        private final List<Round> rounds = new ArrayList<>();
        boolean dealerAdvanced = false;

        public FakeWhistGame(List<Player> players) {
            this.players = players;
        }

        @Override public List<Player> getPlayers() { return players; }

        @Override public void advanceDealer() {
            this.dealerAdvanced = true;
        }

        // --- Mocking dependencies for MenuState transition ---
        @Override public void resetRounds() {}
        @Override public void resetPlayers() {}

        // --- Mocking dependencies for BidState transition ---
        @Override public Player getDealerPlayer() { return players.get(0); }
        @Override public Deck getDeck() { return fakeDeck; }
        @Override public void addRound(Round round) { rounds.add(round); }
        @Override public List<Round> getRounds() { return rounds; }
    }

    /** * Required so BidState doesn't crash when it calls getDeck().deal()
     * during the nextState() transition test.
     */
    static class FakeDeck extends Deck {
        @Override
        public List<List<Card>> deal() {
            List<List<Card>> hands = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                List<Card> hand = new ArrayList<>();
                hand.add(new Card(Suit.SPADES, Rank.ACE)); // Give everyone 1 card to prevent crashes
                hands.add(hand);
            }
            return hands;
        }
    }
}