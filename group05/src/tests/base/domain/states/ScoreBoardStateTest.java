package base.domain.states;

import base.domain.WhistGame;
import base.domain.WhistRules;
import base.domain.commands.GameCommand.*;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.results.CountResults.*;
import base.domain.results.PlayResults.*;
import base.domain.results.GameResult;
import base.domain.round.Round;
import base.storage.GamePersistenceService;
import base.storage.snapshots.SaveMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScoreBoardState Logic & Phase Transitions")
class ScoreBoardStateTest {

    @Mock private WhistGame game;
    @Mock private Player p1, p2, p3, p4, p5;
    @Mock private Round mockRound;
    @Mock private GamePersistenceService mockPersistenceService;

    private final PlayerId id1 = new PlayerId();
    private final PlayerId id5 = new PlayerId();

    private ScoreBoardState state;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(p1.getId()).thenReturn(id1);
        lenient().when(p5.getId()).thenReturn(id5);

        lenient().when(p1.getName()).thenReturn("Alice");
        lenient().when(p2.getName()).thenReturn("Bob");
        lenient().when(p3.getName()).thenReturn("Charlie");
        lenient().when(p4.getName()).thenReturn("Dave");
        lenient().when(p5.getName()).thenReturn("Eve");

        lenient().when(p1.getScore()).thenReturn(10);

        List<Player> corePlayers = List.of(p1, p2, p3, p4);
        lenient().when(game.getAllPlayers()).thenReturn(corePlayers);
        lenient().when(game.getPlayerById(id1)).thenReturn(p1);
        lenient().when(game.getPlayerById(id5)).thenReturn(p5);
        lenient().when(game.getTotalPlayerCount()).thenReturn(4);

        state = new ScoreBoardState(game);

        // Inject Mock Persistence Service via Reflection
        Field psField = ScoreBoardState.class.getDeclaredField("persistenceService");
        psField.setAccessible(true);
        psField.set(state, mockPersistenceService);
    }

    @Nested
    @DisplayName("No-Arg Execution")
    class NoArgExecutionTests {

        @Test
        @DisplayName("Returns ScoreBoardResult when in SHOW phase")
        void returnsScoreBoard() {
            StateStep step = state.executeState();
            assertTrue(step.result() instanceof ScoreBoardResult);
            assertFalse(step.shouldTransition());
        }

        @Test
        @DisplayName("Returns SaveDescriptionResult when in SAVE_DESCRIPTION phase")
        void returnsSaveDescription() {
            state.executeState(new NumberCommand(3)); // Move to SAVE_DESCRIPTION
            StateStep step = state.executeState();
            assertTrue(step.result() instanceof SaveDescriptionResult);
        }
    }

    @Nested
    @DisplayName("SHOW Phase Command Handling")
    class ShowPhaseTests {

        @Test
        @DisplayName("Number 1 & 2 set transition choices")
        void option1and2() {
            StateStep step1 = state.executeState(new NumberCommand(1));
            assertTrue(step1.shouldTransition());

            StateStep step2 = state.executeState(new NumberCommand(2));
            assertTrue(step2.shouldTransition());
        }

        @Test
        @DisplayName("Number 3 transitions to SAVE_DESCRIPTION")
        void option3() {
            StateStep step = state.executeState(new NumberCommand(3));
            assertTrue(step.result() instanceof SaveDescriptionResult);
        }

        @Test
        @DisplayName("Number 4 transitions to REMOVE_ROUND")
        void option4() {
            when(game.getRounds()).thenReturn(List.of(mockRound));
            StateStep step = state.executeState(new NumberCommand(4));
            assertTrue(step.result() instanceof DeleteRoundResult);
        }

        @Test
        @DisplayName("Number 5 transitions to ADD_PLAYER_TYPE")
        void option5() {
            StateStep step = state.executeState(new NumberCommand(5));
            assertTrue(step.result() instanceof AddPlayerResult);
        }

        @Test
        @DisplayName("Number 6 transitions to REMOVE_PLAYER (handles min player guard)")
        void option6() {
            // FIX: Assert that if <= 4 players, the phase transition is blocked and returns ScoreBoardResult
            when(game.getTotalPlayerCount()).thenReturn(4);
            StateStep step4Players = state.executeState(new NumberCommand(6));
            assertTrue(step4Players.result() instanceof ScoreBoardResult);

            // Test with > 4 players (Allows transition to PlayerSelectionResult)
            when(game.getTotalPlayerCount()).thenReturn(5);
            StateStep step5Players = state.executeState(new NumberCommand(6));
            assertTrue(step5Players.result() instanceof PlayerSelectionResult);
        }

        @Test
        @DisplayName("Throws on invalid NumberCommands")
        void invalidNumbers() {
            assertThrows(IllegalStateException.class, () -> state.executeState(new NumberCommand(99)));
        }

        @Test
        @DisplayName("Default branch: Unrelated commands return ScoreBoardResult")
        void defaultBranch() {
            StateStep step = state.executeState(new TextCommand("dummy"));
            assertTrue(step.result() instanceof ScoreBoardResult);
        }
    }

    @Nested
    @DisplayName("SAVE_DESCRIPTION Phase")
    class SaveDescriptionPhaseTests {
        @BeforeEach
        void moveToPhase() {
            state.executeState(new NumberCommand(3));
        }

        @Test
        @DisplayName("TextCommand saves and returns to SHOW")
        void validSave() {
            StateStep step = state.executeState(new TextCommand("My Game"));
            verify(mockPersistenceService).save(eq(game), eq(SaveMode.GAME), eq("My Game"));
            assertTrue(step.result() instanceof ScoreBoardResult);
        }

        @Test
        @DisplayName("Default branch: Unrelated commands stay in phase")
        void defaultBranch() {
            StateStep step = state.executeState(new NumberCommand(1));
            assertTrue(step.result() instanceof SaveDescriptionResult);
        }
    }

    @Nested
    @DisplayName("ADD_PLAYER_TYPE Phase")
    class AddPlayerTypePhaseTests {
        @BeforeEach
        void moveToPhase() {
            state.executeState(new NumberCommand(5));
        }

        @Test
        @DisplayName("Number 1 transitions to ADD_PLAYER_NAME")
        void selectHuman() {
            StateStep step = state.executeState(new NumberCommand(1));
            assertTrue(step.result() instanceof AddHumanPlayerResult);
        }

        @Test
        @DisplayName("Numbers 2, 3, 4 add specific Bot strategies and return to SHOW")
        void selectBots() {
            StateStep step2 = state.executeState(new NumberCommand(2));
            verify(game, times(1)).addPlayer(argThat(p -> p.getName().equals("Smart bot")));
            assertTrue(step2.result() instanceof ScoreBoardResult);

            state.executeState(new NumberCommand(5)); // Re-enter phase
            StateStep step3 = state.executeState(new NumberCommand(3));
            verify(game, times(1)).addPlayer(argThat(p -> p.getName().equals("High bot")));
            assertTrue(step3.result() instanceof ScoreBoardResult);

            state.executeState(new NumberCommand(5)); // Re-enter phase
            StateStep step4 = state.executeState(new NumberCommand(4));
            verify(game, times(1)).addPlayer(argThat(p -> p.getName().equals("Low bot")));
            assertTrue(step4.result() instanceof ScoreBoardResult);
        }

        @Test
        @DisplayName("Throws on invalid NumberCommands")
        void invalidNumbers() {
            assertThrows(IllegalStateException.class, () -> state.executeState(new NumberCommand(99)));
        }

        @Test
        @DisplayName("Default branch: Unrelated commands stay in phase")
        void defaultBranch() {
            StateStep step = state.executeState(new TextCommand("dummy"));
            assertTrue(step.result() instanceof AddPlayerResult);
        }
    }

    @Nested
    @DisplayName("ADD_PLAYER_NAME Phase")
    class AddPlayerNamePhaseTests {
        @BeforeEach
        void moveToPhase() {
            state.executeState(new NumberCommand(5));
            state.executeState(new NumberCommand(1));
        }

        @Test
        @DisplayName("TextCommand adds human player and returns to SHOW")
        void validName() {
            StateStep step = state.executeState(new TextCommand("Frank"));
            verify(game).addPlayer(argThat(p -> p.getName().equals("Frank")));
            assertTrue(step.result() instanceof ScoreBoardResult);
        }

        @Test
        @DisplayName("Default branch: Unrelated commands stay in phase")
        void defaultBranch() {
            StateStep step = state.executeState(new NumberCommand(1));
            assertTrue(step.result() instanceof AddHumanPlayerResult);
        }
    }

    @Nested
    @DisplayName("REMOVE_PLAYER Phase")
    class RemovePlayerPhaseTests {
        @BeforeEach
        void moveToPhase() {
            // FIX: Must simulate > 4 players so the guard allows entry into this phase!
            when(game.getTotalPlayerCount()).thenReturn(5);
            state.executeState(new NumberCommand(6));
        }

        @Test
        @DisplayName("PlayerListCommand removes player and returns to SHOW")
        void validRemoval() {
            StateStep step = state.executeState(new PlayerListCommand(List.of(id5)));
            verify(game).removePlayer(p5);
            assertTrue(step.result() instanceof ScoreBoardResult);
        }

        @Test
        @DisplayName("Empty PlayerListCommand stays in phase to retry")
        void emptyRemoval() {
            StateStep step = state.executeState(new PlayerListCommand(Collections.emptyList()));
            assertTrue(step.result() instanceof PlayerSelectionResult);
            verify(game, never()).removePlayer(any());
        }

        @Test
        @DisplayName("Default branch: Unrelated commands stay in phase")
        void defaultBranch() {
            StateStep step = state.executeState(new NumberCommand(1));
            assertTrue(step.result() instanceof PlayerSelectionResult);
        }
    }

    @Nested
    @DisplayName("REMOVE_ROUND Phase")
    class RemoveRoundPhaseTests {
        @BeforeEach
        void moveToPhase() {
            state.executeState(new NumberCommand(4));
        }

        @Test
        @DisplayName("NumberCommand(0) cancels and returns to SHOW")
        void cancelRemoval() {
            StateStep step = state.executeState(new NumberCommand(0));
            assertTrue(step.result() instanceof ScoreBoardResult);
            verify(game, never()).removeRound(any());
        }

        @Test
        @DisplayName("NumberCommand(any other) stays in phase")
        void stayInPhase() {
            StateStep step = state.executeState(new NumberCommand(99));
            assertTrue(step.result() instanceof DeleteRoundResult);
        }

        @Test
        @DisplayName("RoundCommand executes removal and returns to SHOW")
        void executeRemoval() {
            StateStep step = state.executeState(new RoundCommand(mockRound));
            verify(game).removeRound(mockRound);
            verify(game).recalibrateScores();
            assertTrue(step.result() instanceof ScoreBoardResult);
        }

        @Test
        @DisplayName("Default branch: Unrelated commands stay in phase")
        void defaultBranch() {
            StateStep step = state.executeState(new TextCommand("dummy"));
            assertTrue(step.result() instanceof DeleteRoundResult);
        }
    }

    @Nested
    @DisplayName("Next State Resolution")
    class NextStateTests {

        private void setupBidStateMocks() {
            // FIX: Satisfy the strict requirements of new BidState(game)
            when(game.getPlayers()).thenReturn(List.of(p1, p2, p3, p4));
            when(game.getDealerPlayer()).thenReturn(p4);
            when(game.getNextPlayer(p4)).thenReturn(p1);
            when(game.dealCards()).thenReturn(base.domain.card.Suit.HEARTS);
        }

        @Test
        @DisplayName("Choice 0 (Default) returns this state")
        void choiceDefault() {
            State next = state.nextState();
            assertEquals(state, next);
        }

        @Test
        @DisplayName("Choice 2 returns null (Exit)")
        void choiceExit() {
            state.executeState(new NumberCommand(2));
            assertNull(state.nextState());
        }

        @Test
        @DisplayName("Choice 1 with <= 4 players transitions to BidState directly")
        void choiceContinueStandard() {
            setupBidStateMocks();
            when(game.getTotalPlayerCount()).thenReturn(4);

            state.executeState(new NumberCommand(1));

            State next = state.nextState();
            verify(game).advanceDealer();
            verify(game, never()).rotateActivePlayers();
            assertTrue(next instanceof BidState);
        }

        @Test
        @DisplayName("Choice 1 with > 4 players rotates before transitioning to BidState")
        void choiceContinueRotate() {
            setupBidStateMocks();
            when(game.getTotalPlayerCount()).thenReturn(5);

            state.executeState(new NumberCommand(1));

            State next = state.nextState();
            verify(game).rotateActivePlayers();
            verify(game).advanceDealer();
            assertTrue(next instanceof BidState);
        }
    }
}