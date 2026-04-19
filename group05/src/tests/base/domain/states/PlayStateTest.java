package base.domain.states;

import base.domain.WhistGame;
import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.bid.MiserieBid;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.commands.GameCommand.CardCommand;
import base.domain.commands.GameCommand.NumberCommand;
import base.domain.commands.GameCommand.TextCommand;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.results.PlayResults.*;
import base.domain.round.Round;
import base.domain.trick.Trick;
import base.domain.turn.PlayTurn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayState Gameplay Loop & Transitions")
class PlayStateTest {

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

        lenient().when(game.getPlayerById(id1)).thenReturn(p1);
        lenient().when(game.getPlayerById(id2)).thenReturn(p2);
        lenient().when(game.getPlayerById(id3)).thenReturn(p3);
        lenient().when(game.getPlayerById(id4)).thenReturn(p4);

        lenient().when(game.getCurrentRound()).thenReturn(round);
        lenient().when(round.getCurrentPlayer()).thenReturn(p1);
        lenient().when(round.getTrumpSuit()).thenReturn(Suit.HEARTS);
        lenient().when(game.getPlayerNamesMap()).thenReturn(Map.of(id1, "Alice"));
    }

    @Nested
    @DisplayName("Constructor & Initialization Guards")
    class InitializationTests {

        @Test
        @DisplayName("Successfully creates PlayState with a valid Round")
        void successfulInitialization() {
            assertDoesNotThrow(() -> new PlayState(game));
        }

        @Test
        @DisplayName("Rejects initialization if no active Round exists")
        void throwsOnNullRound() {
            when(game.getCurrentRound()).thenReturn(null);
            IllegalStateException exception = assertThrows(IllegalStateException.class, () -> new PlayState(game));
            assertTrue(exception.getMessage().contains("no currentRound exists"));
        }
    }

    @Nested
    @DisplayName("No-Arg Execution (UI Refresh/Entry)")
    class NoArgExecutionTests {

        @Test
        @DisplayName("Returns PlayCardResult when round is ongoing")
        void staysWhenOngoing() {
            PlayState state = new PlayState(game);
            when(round.isFinished()).thenReturn(false);

            StateStep step = state.executeState();

            assertFalse(step.shouldTransition());
            assertTrue(step.result() instanceof PlayCardResult);
            assertEquals("Alice", ((PlayCardResult) step.result()).player().getName());
        }

        @Test
        @DisplayName("Returns EndOfRoundResult transition if round is already finished")
        void transitionsWhenFinished() {
            PlayState state = new PlayState(game);
            when(round.isFinished()).thenReturn(true);

            StateStep step = state.executeState();

            assertTrue(step.shouldTransition());
            assertTrue(step.result() instanceof EndOfRoundResult);
            assertEquals("Alice", ((EndOfRoundResult) step.result()).name());
        }
    }

    @Nested
    @DisplayName("Open Miserie Visibility Logic")
    class OpenMiserieTests {

        @Test
        @DisplayName("Populates exposed hands when an Open Miserie bid is active")
        void exposesOpenMiserieHands() {
            PlayState state = new PlayState(game);

            Bid mockBid = mock(MiserieBid.class);
            when(mockBid.getType()).thenReturn(BidType.OPEN_MISERIE);
            when(mockBid.getPlayerId()).thenReturn(id2); // Bob plays open miserie

            when(round.getHighestBid()).thenReturn(mockBid);
            when(round.getBids()).thenReturn(List.of(mockBid));

            Card exposedCard = new Card(Suit.SPADES, Rank.ACE);
            when(p2.getHand()).thenReturn(List.of(exposedCard));

            StateStep step = state.executeState();
            PlayCardResult result = (PlayCardResult) step.result();

            assertTrue(result.isOpenMiserie());
            assertEquals(1, result.exposedPlayerNames().size());
            assertEquals("Bob", result.exposedPlayerNames().get(0));
            assertEquals(1, result.formattedExposedHand().size());
            assertTrue(result.formattedExposedHand().get(0).contains(exposedCard));
        }
    }

    @Nested
    @DisplayName("Command Execution & Workflows")
    class CommandExecutionTests {

        private PlayState state;
        private final Card validCard = new Card(Suit.HEARTS, Rank.TWO);

        @BeforeEach
        void init() {
            state = new PlayState(game);
            when(round.isFinished()).thenReturn(false);
        }

        @Test
        @DisplayName("Transitions early if command is sent but round is finished")
        void earlyTransitionIfRoundFinished() {
            when(round.isFinished()).thenReturn(true);
            StateStep step = state.executeState(new TextCommand("dummy"));

            assertTrue(step.shouldTransition());
            assertTrue(step.result() instanceof EndOfRoundResult);
        }

        @Test
        @DisplayName("NumberCommand(0) returns previous trick history if available")
        void returnsTrickHistory() {
            Trick mockTrick = mock(Trick.class);
            when(round.getLastPlayedTrick()).thenReturn(mockTrick);

            StateStep step = state.executeState(new NumberCommand(0));
            assertTrue(step.result() instanceof TrickHistoryResult);
        }

        @Test
        @DisplayName("NumberCommand(0) falls back to PlayCardResult if no trick history exists")
        void fallsBackWithoutHistory() {
            when(round.getLastPlayedTrick()).thenReturn(null);

            StateStep step = state.executeState(new NumberCommand(0));
            assertTrue(step.result() instanceof PlayCardResult);
            assertFalse(step.shouldTransition()); // Switch default branch toStep fallback
        }

        @Test
        @DisplayName("Unhandled commands return a refreshed PlayCardResult")
        void unhandledCommandsFallback() {
            StateStep textStep = state.executeState(new TextCommand("dummy"));
            assertTrue(textStep.result() instanceof PlayCardResult);

            StateStep numberStep = state.executeState(new NumberCommand(99));
            assertTrue(numberStep.result() instanceof PlayCardResult);
        }

        @Test
        @DisplayName("CardCommand falls back if the card played is illegal")
        void illegalCardPlay() {
            Card illegalCard = new Card(Suit.SPADES, Rank.TWO);
            // Player hand does not contain the card, making it illegal via CardMath
            when(p1.getHand()).thenReturn(Collections.emptyList());

            StateStep step = state.executeState(new CardCommand(illegalCard));

            assertTrue(step.result() instanceof PlayCardResult);
            verify(p1, never()).removeCard(any()); // Card was rejected
        }

        @Test
        @DisplayName("CardCommand processes a legal card and ends the Turn")
        void legalCardTurnEnds() {
            when(p1.getHand()).thenReturn(List.of(validCard));

            StateStep step = state.executeState(new CardCommand(validCard));

            assertTrue(step.result() instanceof EndOfTurnResult);
            verify(p1).removeCard(validCard);
            verify(game).notifyTurnPlayed(any(PlayTurn.class));
            verify(round).advanceToNextPlayer();
        }
    }

    @Nested
    @DisplayName("Trick & Round Completion Workflows")
    class GameProgressionTests {

        private PlayState state;
        private final Card c1 = new Card(Suit.HEARTS, Rank.TWO);
        private final Card c2 = new Card(Suit.HEARTS, Rank.THREE);
        private final Card c3 = new Card(Suit.HEARTS, Rank.FOUR);
        private final Card c4 = new Card(Suit.HEARTS, Rank.ACE);

        @BeforeEach
        void init() {
            state = new PlayState(game);
            when(p1.getHand()).thenReturn(List.of(c1));
            when(p2.getHand()).thenReturn(List.of(c2));
            when(p3.getHand()).thenReturn(List.of(c3));
            when(p4.getHand()).thenReturn(List.of(c4));
        }

        @Test
        @DisplayName("Playing 4 cards sequentially completes the Trick and calculates the Winner")
        void trickCompletionWorkflow() {
            // P1
            when(round.getCurrentPlayer()).thenReturn(p1);
            StateStep step1 = state.executeState(new CardCommand(c1));
            assertTrue(step1.result() instanceof EndOfTurnResult);

            // P2
            when(round.getCurrentPlayer()).thenReturn(p2);
            StateStep step2 = state.executeState(new CardCommand(c2));
            assertTrue(step2.result() instanceof EndOfTurnResult);

            // P3
            when(round.getCurrentPlayer()).thenReturn(p3);
            StateStep step3 = state.executeState(new CardCommand(c3));
            assertTrue(step3.result() instanceof EndOfTurnResult);

            // P4 (Final card of the trick)
            when(round.getCurrentPlayer()).thenReturn(p4);
            StateStep step4 = state.executeState(new CardCommand(c4));

            // Assertions
            assertTrue(step4.result() instanceof EndOfTrickResult);
            EndOfTrickResult trickResult = (EndOfTrickResult) step4.result();
            assertEquals(c4, trickResult.card());
            assertEquals("Dave", trickResult.winner(), "P4 played the Ace of Hearts (Trump) and should win");

            verify(round).finalizeTrick(any(Trick.class));
            verify(round, times(3)).advanceToNextPlayer(); // Winner starts next trick, so advance is skipped
        }

        @Test
        @DisplayName("Trick Completion triggers EndOfRoundResult if the Round determines it is finished")
        void roundCompletionWorkflow() {
            // P1, P2, P3 play normally
            when(round.getCurrentPlayer()).thenReturn(p1);
            state.executeState(new CardCommand(c1));

            when(round.getCurrentPlayer()).thenReturn(p2);
            state.executeState(new CardCommand(c2));

            when(round.getCurrentPlayer()).thenReturn(p3);
            state.executeState(new CardCommand(c3));

            // P4 plays the 4th card
            when(round.getCurrentPlayer()).thenReturn(p4);

            // Mock the Round to declare itself finished during the finalizeTrick call
            doAnswer(inv -> {
                when(round.isFinished()).thenReturn(true);
                return null;
            }).when(round).finalizeTrick(any(Trick.class));

            StateStep step4 = state.executeState(new CardCommand(c4));

            // Result must upgrade from EndOfTrick to EndOfRound and Trigger Transition
            assertTrue(step4.result() instanceof EndOfRoundResult);
            assertTrue(step4.shouldTransition());
        }
    }

    @Nested
    @DisplayName("Next State Resolution")
    class NextStateTests {

        @Test
        @DisplayName("Returns itself (stays in PlayState) if round is ongoing")
        void staysInPlayState() {
            PlayState state = new PlayState(game);
            when(round.isFinished()).thenReturn(false);

            State next = state.nextState();
            assertEquals(state, next);
        }
    }
}