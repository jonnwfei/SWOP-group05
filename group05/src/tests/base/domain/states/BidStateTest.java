package base.domain.states;

import base.domain.WhistGame;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.commands.GameCommand.BidCommand;
import base.domain.commands.GameCommand.SuitCommand;
import base.domain.commands.GameCommand.StartGameCommand;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.results.BidResults.BidTurnResult;
import base.domain.results.BidResults.BiddingCompleted;
import base.domain.results.BidResults.ProposalRejected;
import base.domain.results.BidResults.SuitSelectionRequired;
import base.domain.round.Round;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BidState Logic & Transitions")
class BidStateTest {

    @Mock private WhistGame game;
    @Mock private Round round;

    @Mock private Player p1, p2, p3, p4;
    private final PlayerId id1 = new PlayerId();
    private final PlayerId id2 = new PlayerId();
    private final PlayerId id3 = new PlayerId();
    private final PlayerId id4 = new PlayerId();

    @BeforeEach
    void setUpValidGameContext() {
        lenient().when(p1.getId()).thenReturn(id1);
        lenient().when(p2.getId()).thenReturn(id2);
        lenient().when(p3.getId()).thenReturn(id3);
        lenient().when(p4.getId()).thenReturn(id4);

        lenient().when(p1.getName()).thenReturn("Alice");
        lenient().when(p2.getName()).thenReturn("Bob");
        lenient().when(p3.getName()).thenReturn("Charlie");
        lenient().when(p4.getName()).thenReturn("Dave");

        lenient().when(game.getPlayers()).thenReturn(List.of(p1, p2, p3, p4));
        lenient().when(game.getDealerPlayer()).thenReturn(p4);

        // Turn Order: P4 (Dealer) -> P1 -> P2 -> P3 -> P4
        lenient().when(game.getNextPlayer(p4)).thenReturn(p1);
        lenient().when(game.getNextPlayer(p1)).thenReturn(p2);
        lenient().when(game.getNextPlayer(p2)).thenReturn(p3);
        lenient().when(game.getNextPlayer(p3)).thenReturn(p4);

        lenient().when(game.dealCards()).thenReturn(Suit.HEARTS);
        lenient().when(game.getCurrentRound()).thenReturn(round);

        lenient().when(round.getCurrentPlayer()).thenReturn(p1);

        // Player lookups
        lenient().when(game.getPlayerById(id1)).thenReturn(p1);
        lenient().when(game.getPlayerById(id2)).thenReturn(p2);
        lenient().when(game.getPlayerById(id3)).thenReturn(p3);
        lenient().when(game.getPlayerById(id4)).thenReturn(p4);
    }

    @Nested
    @DisplayName("Constructor & Initialization Guards")
    class InitializationTests {

        @Test
        @DisplayName("Successfully initializes a standard bidding phase without forced bids")
        void successfulInitialization() {
            BidState state = new BidState(game);

            verify(game).initializeNextRound(p1);
            verify(game).notifyRoundStarted();

            StateStep initialStep = state.executeState();
            assertTrue(initialStep.result() instanceof BidTurnResult);
            assertEquals("Alice", ((BidTurnResult) initialStep.result()).playerName());
        }

        @Test
        @DisplayName("Rejects initialization with invalid player configurations")
        void invalidPlayerConfigs() {
            when(game.getPlayers()).thenReturn(null);
            assertThrows(IllegalArgumentException.class, () -> new BidState(game));

            when(game.getPlayers()).thenReturn(List.of(p1, p2, p3));
            assertThrows(IllegalArgumentException.class, () -> new BidState(game));

            when(game.getPlayers()).thenReturn(Arrays.asList(p1, p2, null, p4));
            assertThrows(IllegalArgumentException.class, () -> new BidState(game));
        }

        @Test
        @DisplayName("Rejects initialization if dealer or trump suit are missing")
        void missingDealerOrTrump() {
            when(game.getPlayers()).thenReturn(List.of(p1, p2, p3, p4));

            when(game.getDealerPlayer()).thenReturn(null);
            assertThrows(IllegalStateException.class, () -> new BidState(game));

            when(game.getDealerPlayer()).thenReturn(p4);
            when(game.dealCards()).thenReturn(null);
            assertThrows(IllegalStateException.class, () -> new BidState(game));
        }
    }

    @Nested
    @DisplayName("Forced Bids (Troel & Troela)")
    class ForcedBidTests {

        @Test
        @DisplayName("TROEL (3 Aces) is automatically applied and skips the forced player's turn")
        void appliesTroel() {
            when(p1.hasCard(any(Card.class))).thenAnswer(inv -> {
                Card c = inv.getArgument(0);
                return c.rank() == Rank.ACE && c.suit() != Suit.SPADES;
            });

            BidState state = new BidState(game);

            StateStep step = state.executeState();
            assertEquals("Bob", ((BidTurnResult) step.result()).playerName());
            verify(game).notifyBidPlaced(argThat(turn -> turn.bidType() == BidType.TROEL));
        }

        @Test
        @DisplayName("TROELA (4 Aces) is automatically applied")
        void appliesTroela() {
            when(p3.hasCard(argThat(c -> c.rank() == Rank.ACE))).thenReturn(true);

            BidState state = new BidState(game);

            verify(game).notifyBidPlaced(argThat(turn -> turn.bidType() == BidType.TROELA));
        }
    }

    @Nested
    @DisplayName("Command Execution & Bidding Workflows")
    class CommandExecutionTests {

        @Test
        @DisplayName("Rejects invalid commands")
        void invalidCommands() {
            BidState state = new BidState(game);

            assertThrows(IllegalArgumentException.class, () -> state.executeState(null));
            assertThrows(IllegalStateException.class, () -> state.executeState(new StartGameCommand()));
        }

        @Test
        @DisplayName("Standard ALL PASS workflow emits BidTurnResults until complete")
        void allPassWorkflow() {
            BidState state = new BidState(game);

            StateStep step1 = state.executeState(new BidCommand(BidType.PASS));
            assertTrue(step1.result() instanceof BidTurnResult);
            assertEquals("Bob", ((BidTurnResult) step1.result()).playerName(), "Turn passes to Bob");

            StateStep step2 = state.executeState(new BidCommand(BidType.PASS));
            assertTrue(step2.result() instanceof BidTurnResult);
            assertEquals("Charlie", ((BidTurnResult) step2.result()).playerName(), "Turn passes to Charlie");

            StateStep step3 = state.executeState(new BidCommand(BidType.PASS));
            assertTrue(step3.result() instanceof BidTurnResult);
            assertEquals("Dave", ((BidTurnResult) step3.result()).playerName(), "Turn passes to Dave");

            StateStep step4 = state.executeState(new BidCommand(BidType.PASS));
            assertTrue(step4.result() instanceof BiddingCompleted);
            assertTrue(step4.shouldTransition());

            State next = state.nextState();
            verify(round).abortWithAllPass(anyList());
            assertTrue(next instanceof BidState, "All pass triggers a new BidState");
        }

        @Test
        @DisplayName("Suit Selection Workflow (Abondance)")
        void suitSelectionWorkflow() {
            BidState state = new BidState(game);

            StateStep step = state.executeState(new BidCommand(BidType.ABONDANCE_9));
            assertTrue(step.result() instanceof SuitSelectionRequired);

            assertThrows(IllegalStateException.class, () -> state.executeState(new BidCommand(BidType.PASS)));

            StateStep suitStep = state.executeState(new SuitCommand(Suit.SPADES));

            assertTrue(suitStep.result() instanceof BidTurnResult);
            assertEquals("Bob", ((BidTurnResult) suitStep.result()).playerName());
        }

        @Test
        @DisplayName("Pre-Supplied Suit (Bot behavior)")
        void preSuppliedSuit() {
            BidState state = new BidState(game);

            StateStep step = state.executeState(new BidCommand(BidType.ABONDANCE_9, Suit.CLUBS));

            assertTrue(step.result() instanceof BidTurnResult, "Should seamlessly process and move to next player");
            assertEquals("Bob", ((BidTurnResult) step.result()).playerName());
        }

        @Test
        @DisplayName("Rejects invalid SuitCommands")
        void invalidSuitCommands() {
            BidState state = new BidState(game);

            assertThrows(IllegalStateException.class, () -> state.executeState(new SuitCommand(Suit.HEARTS)));

            state.executeState(new BidCommand(BidType.ABONDANCE_9));
            assertThrows(IllegalArgumentException.class, () -> state.executeState(new SuitCommand(null)));
        }
    }

    @Nested
    @DisplayName("Legality Validation Rules")
    class LegalityTests {

        @Test
        @DisplayName("Enforces Bid Hierarchy and constraints")
        void legalityConstraints() {
            BidState state = new BidState(game);

            assertThrows(IllegalArgumentException.class, () -> state.executeState(new BidCommand(null)));
            assertThrows(IllegalArgumentException.class, () -> state.executeState(new BidCommand(BidType.TROEL)), "Troel is forced only");
            assertThrows(IllegalArgumentException.class, () -> state.executeState(new BidCommand(BidType.ACCEPTANCE)), "Cannot accept without proposal");
            assertThrows(IllegalArgumentException.class, () -> state.executeState(new BidCommand(BidType.SOLO_PROPOSAL)), "Cannot solo-proposal unless rejecting");

            state.executeState(new BidCommand(BidType.PROPOSAL));

            assertThrows(IllegalArgumentException.class, () -> state.executeState(new BidCommand(BidType.PROPOSAL)));

            state.executeState(new BidCommand(BidType.PASS));

            state.executeState(new BidCommand(BidType.MISERIE));

            assertDoesNotThrow(() -> state.executeState(new BidCommand(BidType.MISERIE)));
        }
    }

    @Nested
    @DisplayName("Rejected Proposal Workflow")
    class RejectedProposalTests {

        @Test
        @DisplayName("Handles end of bidding with a rejected proposal")
        void rejectedProposalFlow() {
            BidState state = new BidState(game);

            state.executeState(new BidCommand(BidType.PROPOSAL));
            state.executeState(new BidCommand(BidType.PASS));
            state.executeState(new BidCommand(BidType.PASS));

            StateStep step = state.executeState(new BidCommand(BidType.PASS));
            assertTrue(step.result() instanceof ProposalRejected);

            assertThrows(IllegalArgumentException.class, () -> state.executeState(new BidCommand(null)));
            assertThrows(IllegalArgumentException.class, () -> state.executeState(new BidCommand(BidType.ABONDANCE_9)), "Must be PASS or SOLO_PROPOSAL");

            StateStep finalStep = state.executeState(new BidCommand(BidType.SOLO_PROPOSAL));
            assertTrue(finalStep.result() instanceof BiddingCompleted);

            assertTrue(state.nextState() instanceof PlayState);
        }
    }

    @Nested
    @DisplayName("Defensive Guard Tests (Reflection Required)")
    class DefensiveGuardTests {

        @Test
        @DisplayName("nextState() throws if bidding is incomplete")
        void nextStateIncomplete() {
            BidState state = new BidState(game);
            assertThrows(IllegalStateException.class, state::nextState);
        }

        @Test
        @DisplayName("executeState() throws if called after completion")
        void bidAfterComplete() throws Exception {
            BidState state = new BidState(game);

            for(int i=0; i<4; i++) state.executeState(new BidCommand(BidType.PASS));

            assertThrows(IllegalStateException.class, () -> state.executeState(new BidCommand(BidType.PASS)));
        }

        @Test
        @DisplayName("Defensive Guards: Corrupt Proposal Resolution")
        void corruptProposalResolution() throws Exception {
            BidState state = new BidState(game);

            // FIX: Use the new "currentHighestBid" field and instantiate a real Bid
            setPrivateField(state, "currentHighestBid", BidType.PROPOSAL.instantiate(id1, null));

            assertThrows(IllegalStateException.class, () -> invokePrivateHandleMethod(state, "handleEndOfBidding"));
            assertThrows(IllegalStateException.class, () -> invokePrivateHandleMethod(state, "handleRejectedProposal", BidType.PASS));
        }

        @Test
        @DisplayName("Defensive Guards: Corrupt Winning Bid Preparation")
        void corruptPlayStatePrep() throws Exception {
            BidState state = new BidState(game);

            // FIX: Since we store the Bid now, the risk is different. We test the new guard branches in setRoundReadyForPlayState!

            // Guard 1: Null highest bid
            setPrivateField(state, "currentHighestBid", null);
            assertThrows(IllegalStateException.class, () -> invokePrivateHandleMethod(state, "setRoundReadyForPlayState"));

            // Guard 2: PASS winning bid
            setPrivateField(state, "currentHighestBid", BidType.PASS.instantiate(id1, null));
            assertThrows(IllegalStateException.class, () -> invokePrivateHandleMethod(state, "setRoundReadyForPlayState"));

            // Guard 3: Unresolved PROPOSAL
            setPrivateField(state, "currentHighestBid", BidType.PROPOSAL.instantiate(id1, null));
            assertThrows(IllegalStateException.class, () -> invokePrivateHandleMethod(state, "setRoundReadyForPlayState"));
        }
    }

    @Nested
    @DisplayName("First Player Resolution")
    class FirstPlayerResolutionTests {

        @Test
        @DisplayName("Abondance/Solo Winner leads first")
        void abondanceWinnerLeads() {
            BidState state = new BidState(game);

            state.executeState(new BidCommand(BidType.PASS));
            state.executeState(new BidCommand(BidType.ABONDANCE_9, Suit.SPADES));
            state.executeState(new BidCommand(BidType.PASS));
            state.executeState(new BidCommand(BidType.PASS));

            state.nextState();

            verify(round).startPlayPhase(anyList(), any(), eq(Suit.SPADES), eq(p2));
        }

        @Test
        @DisplayName("Troel: Partner of the Troel bidder leads first")
        void troelPartnerLeads() {
            when(p1.hasCard(any(Card.class))).thenAnswer(inv -> {
                Card c = inv.getArgument(0);
                return c.rank() == Rank.ACE && c.suit() != Suit.SPADES;
            });
            when(p3.hasCard(argThat(c -> c.rank() == Rank.ACE && c.suit() == Suit.SPADES))).thenReturn(true);

            BidState state = new BidState(game);

            state.executeState(new BidCommand(BidType.PASS));
            state.executeState(new BidCommand(BidType.PASS));
            state.executeState(new BidCommand(BidType.PASS));

            state.nextState();

            verify(round).startPlayPhase(anyList(), any(), eq(Suit.SPADES), eq(p3));
        }
    }

    private void setPrivateField(Object object, String fieldName, Object value) throws Exception {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }

    private void invokePrivateHandleMethod(Object object, String methodName) throws Exception {
        java.lang.reflect.Method method = object.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        try {
            method.invoke(object);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    private void invokePrivateHandleMethod(Object object, String methodName, BidType arg) throws Exception {
        java.lang.reflect.Method method = object.getClass().getDeclaredMethod(methodName, BidType.class);
        method.setAccessible(true);
        try {
            method.invoke(object, arg);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }
}