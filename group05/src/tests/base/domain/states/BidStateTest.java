package base.domain.states;

import base.domain.WhistGame;
import base.domain.actions.ContinueAction;
import base.domain.actions.NumberAction;
import base.domain.bid.BidType;
import base.domain.deck.Deck;
import base.domain.events.ErrorEvent;
import base.domain.events.GameEvent;
import base.domain.events.bidevents.BidTurnEvent;
import base.domain.events.bidevents.BiddingCompleteEvent;
import base.domain.events.bidevents.RejectedProposalEvent;
import base.domain.events.bidevents.SuitPromptEvent;
import base.domain.strategy.HighBotStrategy;
import base.domain.player.Player;
import base.domain.round.Round;
import base.domain.card.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BidStateTest {

    private FakeWhistGame fakeGame;
    private BidState bidState;
    private FakePlayer p1, p2, p3, p4;

    @BeforeEach
    void setUp() {
        // 1. Initialize our manual fake objects
        p1 = new FakePlayer("Player 1", true);
        p2 = new FakePlayer("Player 2", true);
        p3 = new FakePlayer("Player 3", true);
        p4 = new FakePlayer("Player 4", true); // P4 is dealer, P1 acts first

        List<Player> players = Arrays.asList(p1, p2, p3, p4);
        fakeGame = new FakeWhistGame(players, p4);

        // 2. Instantiate the State with the fake game
        bidState = new BidState(fakeGame);
    }

    @Test
    void testInitialization_DealsCardsAndCreatesRound() {
        // Verify the fake deck was dealt exactly once during BidState construction
        assertTrue(fakeGame.fakeDeck.wasDealt, "Deck should be dealt upon initialization");
        assertEquals(1, fakeGame.rounds.size(), "A new Round should be added to the game");

        // Ensure Player 1 is prompted first
        GameEvent<?> event = bidState.executeState(new ContinueAction());
        assertInstanceOf(BidTurnEvent.class, event);
        assertEquals("Player 1", ((BidTurnEvent) event).playerName());
    }

    @Test
    void testExecuteState_LegalBid_AdvancesTurn() {
        // Find the integer index for PASS
        int passIndex = getBidIndex(BidType.PASS);

        GameEvent<?> event = bidState.executeState(new NumberAction(passIndex));

        assertInstanceOf(BidTurnEvent.class, event);
        assertEquals("Player 2", ((BidTurnEvent) event).playerName(), "Turn should advance to P2 after P1 passes");
    }

    @Test
    void testExecuteState_IllegalBid_ReturnsErrorEvent() {
        // ACCEPTANCE is illegal if no PROPOSAL was made
        int acceptanceIndex = getBidIndex(BidType.ACCEPTANCE);

        GameEvent<?> event = bidState.executeState(new NumberAction(acceptanceIndex));

        assertInstanceOf(ErrorEvent.class, event, "Should return ErrorEvent for illegal bid hierarchy");
    }

    @Test
    void testExecuteState_BidRequiringSuit_ReturnsSuitPrompt() {
        int soloIndex = getBidIndex(BidType.SOLO); // Assuming SOLO requires a suit

        GameEvent<?> event = bidState.executeState(new NumberAction(soloIndex));

        assertInstanceOf(SuitPromptEvent.class, event);
        SuitPromptEvent suitEvent = (SuitPromptEvent) event;
        assertEquals("Player 1", suitEvent.playerName());
        assertEquals(BidType.SOLO, suitEvent.pendingType());
    }

    @Test
    void testExecuteState_BotAutoPasses() {
        // Change P1 to a BOT (doesn't require confirmation)
        p1.requiresConfirmation = false;

        // Send a generic action to trigger the state loop
        GameEvent<?> event = bidState.executeState(new ContinueAction());

        // P1 should be skipped, returning the turn event for P2
        assertInstanceOf(BidTurnEvent.class, event);
        assertEquals("Player 2", ((BidTurnEvent) event).playerName(), "Bot should auto-pass and advance turn to next player");
    }

    @Test
    void testNextState_AllPass_CreatesNewBidState() {
        int passIndex = getBidIndex(BidType.PASS);

        // All 4 players pass
        bidState.executeState(new NumberAction(passIndex));
        bidState.executeState(new NumberAction(passIndex));
        bidState.executeState(new NumberAction(passIndex));
        bidState.executeState(new NumberAction(passIndex));

        State next = bidState.nextState();

        assertInstanceOf(BidState.class, next, "If all pass, state should restart as BidState");
        assertTrue(fakeGame.fakeDeck.wasShuffled, "Deck should be reshuffled when everyone passes");
    }

    // --- Helper Methods ---
    private int getBidIndex(BidType type) {
        return Arrays.asList(BidType.values()).indexOf(type) + 1;
    }

    // =========================================================================
    // MANUAL FAKES (Stubbing dependencies to isolate BidState logic)
    // =========================================================================

    /**
     * Fake Player that overrides getters to return controlled test data.
     * Note: Adjust 'extends Player' to 'implements Player' if it's an interface.
     */
    static class FakePlayer extends Player {
        String testName;
        boolean requiresConfirmation;
        List<Card> fakeHand = new ArrayList<>();

        public FakePlayer(String name, boolean isHuman) {
            super(new HighBotStrategy(), name); // Assuming Player has a String constructor
            this.testName = name;
            this.requiresConfirmation = isHuman;
            // Add a dummy card so the 'last card suit' check in dealCards() doesn't crash
            this.fakeHand.add(new Card(Suit.SPADES, Rank.ACE));
        }

        @Override public String getName() { return testName; }
        @Override public boolean getRequiresConfirmation() { return requiresConfirmation; }
        @Override public List<Card> getHand() { return fakeHand; }
        @Override public void setHand(List<Card> hand) { this.fakeHand = hand; }
        @Override public void flushHand() { this.fakeHand.clear(); }
    }

    /** Fake Game to intercept round additions and supply our Fake Deck/Players. */
    static class FakeWhistGame extends WhistGame {
        List<Player> players;
        Player dealer;
        FakeDeck fakeDeck = new FakeDeck();
        List<Round> rounds = new ArrayList<>();

        public FakeWhistGame(List<Player> players, Player dealer) {
            super(); // Assuming an empty constructor exists
            this.players = players;
            this.dealer = dealer;
        }

        @Override public List<Player> getPlayers() { return players; }
        @Override public Player getDealerPlayer() { return dealer; }
        @Override public Deck getDeck() { return fakeDeck; }
        @Override public void addRound(Round round) { rounds.add(round); }
        @Override public List<Round> getRounds() { return rounds; }
        @Override public Round getCurrentRound() {
            return rounds.isEmpty() ? null : rounds.getLast();
        }
    }

    /** Fake Deck to bypass complex 52-card dealing logic. */
    static class FakeDeck extends Deck {
        boolean wasDealt = false;
        boolean wasShuffled = false;

        @Override
        public List<List<Card>> deal() {
            wasDealt = true;
            // Wrap in ArrayList so flushHand().clear() works
            return Arrays.asList(
                    new ArrayList<>(List.of(new Card(Suit.HEARTS, Rank.ACE))),
                    new ArrayList<>(List.of(new Card(Suit.HEARTS, Rank.KING))),
                    new ArrayList<>(List.of(new Card(Suit.HEARTS, Rank.QUEEN))),
                    new ArrayList<>(List.of(new Card(Suit.HEARTS, Rank.JACK)))
            );
        }

        @Override
        public void shuffle() {
            wasShuffled = true;
        }
    }

    @Test
    void testExecuteState_OutOfBoundsBid_ReturnsErrorEvent() {
        // Provide a choice way outside the valid BidType.values() range
        GameEvent<?> event = bidState.executeState(new NumberAction(999));
        assertInstanceOf(ErrorEvent.class, event, "Should return ErrorEvent for out-of-bounds bid choice");
    }

    @Test
    void testExecuteState_OutOfBoundsSuit_ReturnsErrorEvent() {
        // 1. Trigger the pending suit state
        int soloIndex = getBidIndex(BidType.SOLO);
        bidState.executeState(new NumberAction(soloIndex));

        // 2. Provide a suit choice outside the valid Suit.values() range
        GameEvent<?> event = bidState.executeState(new NumberAction(999));
        assertInstanceOf(ErrorEvent.class, event, "Should return ErrorEvent for out-of-bounds suit choice");
    }

    @Test
    void testExecuteState_RejectedProposal_TriggersEventAndResolvesToPass() {
        int proposalIdx = getBidIndex(BidType.PROPOSAL);
        int passIdx = getBidIndex(BidType.PASS);

        // P1 Proposes
        bidState.executeState(new NumberAction(proposalIdx));
        // P2, P3, P4 Pass
        bidState.executeState(new NumberAction(passIdx));
        bidState.executeState(new NumberAction(passIdx));
        GameEvent<?> event = bidState.executeState(new NumberAction(passIdx));

        // Coverage for Context A initialization
        assertInstanceOf(RejectedProposalEvent.class, event, "If bidding completes with PROPOSAL as highest, prompt original proposer");

        // P1 decides to Pass (Choice 1)
        GameEvent<?> resolveEvent = bidState.executeState(new NumberAction(1));
        assertInstanceOf(BiddingCompleteEvent.class, resolveEvent, "Choosing 1 should replace proposal with PASS and complete bidding");
    }

    @Test
    void testExecuteState_RejectedProposal_ResolvesToSoloProposal() {
        int proposalIdx = getBidIndex(BidType.PROPOSAL);
        int passIdx = getBidIndex(BidType.PASS);

        // P1 Proposes, others pass
        bidState.executeState(new NumberAction(proposalIdx));
        bidState.executeState(new NumberAction(passIdx));
        bidState.executeState(new NumberAction(passIdx));
        bidState.executeState(new NumberAction(passIdx));

        // P1 decides to play Solo Proposal (Choice 2)
        GameEvent<?> resolveEvent = bidState.executeState(new NumberAction(2));
        assertInstanceOf(BiddingCompleteEvent.class, resolveEvent, "Choosing 2 should replace proposal with SOLO_PROPOSAL and complete bidding");
    }

    @Test
    void testExecuteState_RejectedProposal_InvalidChoice_ReturnsError() {
        // Setup rejected proposal state
        bidState.executeState(new NumberAction(getBidIndex(BidType.PROPOSAL)));
        bidState.executeState(new NumberAction(getBidIndex(BidType.PASS)));
        bidState.executeState(new NumberAction(getBidIndex(BidType.PASS)));
        bidState.executeState(new NumberAction(getBidIndex(BidType.PASS)));

        // P1 inputs invalid choice (3)
        GameEvent<?> resolveEvent = bidState.executeState(new NumberAction(3));
        assertInstanceOf(ErrorEvent.class, resolveEvent, "Invalid resolution choice should return ErrorEvent");
    }

    @Test
    void testNextState_AbondanceWins_SetsBidderAsFirstPlayer() {
        int passIdx = getBidIndex(BidType.PASS);
        int abondanceIdx = getBidIndex(BidType.ABONDANCE_9);

        // P1 Passes
        bidState.executeState(new NumberAction(passIdx));

        // P2 Bids Abondance and chooses Suit 1
        bidState.executeState(new NumberAction(abondanceIdx));
        bidState.executeState(new NumberAction(1));

        // P3, P4 Pass
        bidState.executeState(new NumberAction(passIdx));
        bidState.executeState(new NumberAction(passIdx));

        bidState.nextState();

        // Normally P1 leads (left of dealer P4). But since P2 bid Abondance, P2 should lead.
        assertEquals("Player 2", fakeGame.getCurrentRound().getCurrentPlayer().getName(),
                "Abondance winner should be set as the first player for the PlayState");
    }
}