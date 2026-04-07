package base.domain.states;

import base.domain.WhistGame;
import base.domain.actions.ContinueAction;
import base.domain.actions.NumberAction;
import base.domain.actions.TextAction;
import base.domain.bid.Bid;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.events.ErrorEvent;
import base.domain.events.GameEvent;
import base.domain.events.playevents.*;
import base.domain.player.HumanStrategy;
import base.domain.player.Player;
import base.domain.round.Round;
import base.domain.trick.Trick;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlayStateTest {

    private FakeWhistGame fakeGame;
    private FakeRound fakeRound;
    private PlayState playState;
    private FakePlayer p1, p2, p3, p4;

    @BeforeEach
    void setUp() {
        p1 = new FakePlayer("Alice", true);
        p2 = new FakePlayer("Bob", true);
        p3 = new FakePlayer("Charlie", true);
        p4 = new FakePlayer("Diana", true);
        List<Player> players = Arrays.asList(p1, p2, p3, p4);

        fakeRound = new FakeRound(players, p1);
        fakeRound.setTrumpSuit(Suit.SPADES);

        fakeGame = new FakeWhistGame(players, fakeRound);
        playState = new PlayState(fakeGame);
    }

    @Test
    void testConstructor_ThrowsExceptionWhenNoRoundExists() {
        FakeWhistGame emptyGame = new FakeWhistGame(new ArrayList<>(), null);
        assertThrows(IllegalStateException.class, () -> new PlayState(emptyGame),
                "PlayState should instantly crash if there is no active round.");
    }

    @Test
    void testHumanTurn_FullCycle_EndOfTurn() {
        // Use testHand directly to bypass Player encapsulation
        p1.testHand.add(new Card(Suit.CLUBS, Rank.ACE));

        GameEvent<?> promptEvent = playState.executeState(new ContinueAction());
        assertInstanceOf(InitiateTurnEvent.class, promptEvent);

        GameEvent<?> revealEvent = playState.executeState(new ContinueAction());
        assertInstanceOf(PickCardEvent.class, revealEvent);

        GameEvent<?> playEvent = playState.executeState(new NumberAction(1));

        assertInstanceOf(EndOfTurnEvent.class, playEvent);
        assertEquals(0, p1.testHand.size(), "The card should be physically removed from Alice's hand");
    }

    @Test
    void testBotTurn_PlaysAutomatically_EndOfTurn() {
        p1.requiresConfirmation = false;
        p1.testHand.add(new Card(Suit.HEARTS, Rank.TWO));

        GameEvent<?> event = playState.executeState(new ContinueAction());

        assertInstanceOf(EndOfTurnEvent.class, event);
        assertEquals(0, p1.testHand.size(), "Bot should automatically play and remove the card");
    }

    @Test
    void testTrickCompletion_ReturnsEndOfTrickEvent() {
        p1.requiresConfirmation = false; p1.testHand.add(new Card(Suit.CLUBS, Rank.TWO));
        p2.requiresConfirmation = false; p2.testHand.add(new Card(Suit.CLUBS, Rank.THREE));
        p3.requiresConfirmation = false; p3.testHand.add(new Card(Suit.CLUBS, Rank.FOUR));
        p4.requiresConfirmation = true;  p4.testHand.add(new Card(Suit.CLUBS, Rank.FIVE));

        playState.executeState(new ContinueAction()); // P1 plays
        playState.executeState(new ContinueAction()); // P2 plays
        playState.executeState(new ContinueAction()); // P3 plays

        playState.executeState(new ContinueAction()); // P4 Prompt
        playState.executeState(new ContinueAction()); // P4 Reveal
        GameEvent<?> event = playState.executeState(new NumberAction(1)); // P4 Plays

        assertInstanceOf(EndOfTrickEvent.class, event);
        assertEquals(1, fakeRound.getTricks().size(), "The round should have saved the completed trick");
    }

    @Test
    void testRoundCompletion_ReturnsEndOfRoundEvent_AndTransitions() {
        for (int i = 0; i < 12; i++) {
            fakeRound.fakeTricks.add(new Trick(p1, Suit.SPADES));
        }

        p1.requiresConfirmation = false; p1.testHand.add(new Card(Suit.HEARTS, Rank.TWO));
        p2.requiresConfirmation = false; p2.testHand.add(new Card(Suit.HEARTS, Rank.THREE));
        p3.requiresConfirmation = false; p3.testHand.add(new Card(Suit.HEARTS, Rank.FOUR));
        p4.requiresConfirmation = true;  p4.testHand.add(new Card(Suit.HEARTS, Rank.FIVE));

        playState.executeState(new ContinueAction());
        playState.executeState(new ContinueAction());
        playState.executeState(new ContinueAction());
        playState.executeState(new ContinueAction());
        playState.executeState(new ContinueAction());

        GameEvent<?> event = playState.executeState(new NumberAction(1));

        assertInstanceOf(EndOfRoundEvent.class, event);
        assertInstanceOf(ScoreBoardState.class, playState.nextState());
    }

    @Test
    void testInvalidInput_ReturnsErrorEvent() {
        p1.testHand.add(new Card(Suit.CLUBS, Rank.ACE));

        playState.executeState(new ContinueAction());
        playState.executeState(new ContinueAction());

        assertInstanceOf(ErrorEvent.class, playState.executeState(new TextAction("Cheat")));
        assertInstanceOf(ErrorEvent.class, playState.executeState(new NumberAction(2)));
        assertInstanceOf(ErrorEvent.class, playState.executeState(new NumberAction(-1)));
    }

    @Test
    void testViewLastTrick_OptionZero() {
        playState.executeState(new ContinueAction());
        playState.executeState(new ContinueAction());

        assertInstanceOf(ErrorEvent.class, playState.executeState(new NumberAction(0)));

        fakeRound.fakeTricks.add(new Trick(p1, Suit.SPADES));
        assertInstanceOf(LastTrickEvent.class, playState.executeState(new NumberAction(0)));
    }

    @Test
    void testIllegalCardPlay_CatchesException_ReturnsPickCardEvent() {
        // Give P1 a Club and a Heart.
        p1.testHand.add(new Card(Suit.CLUBS, Rank.TWO));
        p1.testHand.add(new Card(Suit.HEARTS, Rank.TWO));

        // Let P4 lead the trick so the turn naturally advances to P1 next
        p4.requiresConfirmation = false;
        p4.testHand.add(new Card(Suit.CLUBS, Rank.ACE));

        // Set P4 as current player to lead the trick
        fakeRound.setCurrentPlayer(p4);
        playState = new PlayState(fakeGame); // Re-init to grab P4 as starter
        playState.executeState(new ContinueAction()); // P4 plays Club

        // Now the round correctly advanced to P1.
        playState.executeState(new ContinueAction()); // P1 Prompt
        playState.executeState(new ContinueAction()); // P1 Reveal

        // P1 tries to play index 2 (The Heart). They HAVE a Club, so this is illegal (reneging).
        GameEvent<?> event = playState.executeState(new NumberAction(2));

        assertInstanceOf(PickCardEvent.class, event, "Illegal plays should be caught and return the PickCard screen again");
    }


    // =========================================================================
    // BULLETPROOF MANUAL FAKES
    // =========================================================================

    static class FakePlayer extends Player {
        boolean requiresConfirmation;
        public List<Card> testHand = new ArrayList<>();

        public FakePlayer(String name, boolean human) {
            super(new HumanStrategy(), name);
            this.requiresConfirmation = human;
        }

        @Override public boolean getRequiresConfirmation() { return requiresConfirmation; }
        @Override public List<Card> getHand() { return testHand; }
        @Override public void removeCard(Card card) { testHand.remove(card); }

        // ADD THIS OVERRIDE: So Trick.playCard() knows we actually have the leading suit!
        @Override
        public Boolean hasSuit(Suit suit) {
            return testHand.stream().anyMatch(c -> c.suit() == suit);
        }

        @Override
        public Card chooseCard(Suit lead) {
            if (testHand.isEmpty()) throw new IllegalStateException("Bot tried to play with an empty hand");
            return testHand.get(0);
        }
    }

    static class FakeWhistGame extends WhistGame {
        List<Player> players;
        Round currentRound;

        public FakeWhistGame(List<Player> p, Round r) {
            this.players = p;
            this.currentRound = r;
        }

        @Override public List<Player> getPlayers() { return players; }
        @Override public Round getCurrentRound() { return currentRound; }
    }

    static class FakeRound extends Round {
        List<Trick> fakeTricks = new ArrayList<>();
        Bid fakeBid;
        Player currentPlayer;
        Suit Trump;

        public FakeRound(List<Player> players, Player start) {
            super(players, start, 1);
        }

        @Override public List<Trick> getTricks() { return fakeTricks; }
        @Override public Bid getHighestBid() { return fakeBid; }
        public void setHighestBid(Bid bid) { this.fakeBid = bid; }
        public void setCurrentPlayer(Player player){this.currentPlayer = player;}
        public void setTrumpSuit(Suit trump){this.Trump = trump;}

        @Override
        public void registerCompletedTrick(Trick trick) {
            fakeTricks.add(trick);
        }

        @Override
        public Trick getLastPlayedTrick() {
            return fakeTricks.isEmpty() ? null : fakeTricks.getLast();
        }
    }
}