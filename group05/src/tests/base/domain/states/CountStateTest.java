package base.domain.states;

import base.domain.WhistGame;
import base.domain.bid.BidType;
import base.domain.card.Suit;
import base.domain.commands.GameCommand;
import base.domain.commands.GameCommand.*;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.results.CountResults.*;
import base.domain.results.GameResult;
import base.domain.round.Round;
import base.storage.GamePersistenceService;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CountState Logic & Phase Transitions")
class CountStateTest {

    @Mock private WhistGame game;
    @Mock private Player p1, p2, p3, p4, p5;
    @Mock private Round mockRound;
    @Mock private GamePersistenceService mockPersistenceService;

    private final PlayerId id1 = new PlayerId();
    private final PlayerId id2 = new PlayerId();
    private final PlayerId id5 = new PlayerId();

    private CountState state;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(p1.getId()).thenReturn(id1);
        lenient().when(p2.getId()).thenReturn(id2);
        lenient().when(p5.getId()).thenReturn(id5);

        lenient().when(p1.getName()).thenReturn("Alice");
        lenient().when(p2.getName()).thenReturn("Bob");
        lenient().when(p3.getName()).thenReturn("Charlie");
        lenient().when(p4.getName()).thenReturn("Dave");
        lenient().when(p5.getName()).thenReturn("Eve");

        lenient().when(p1.getScore()).thenReturn(10);

        List<Player> corePlayers = List.of(p1, p2, p3, p4);
        lenient().when(game.getPlayers()).thenReturn(corePlayers);
        lenient().when(game.getAllPlayers()).thenReturn(corePlayers);
        lenient().when(game.getPlayerById(id1)).thenReturn(p1);
        lenient().when(game.getPlayerById(id2)).thenReturn(p2);
        lenient().when(game.getPlayerById(id5)).thenReturn(p5);

        state = new CountState(game);

        // Inject Mock Persistence Service via Reflection
        Field psField = CountState.class.getDeclaredField("persistenceService");
        psField.setAccessible(true);
        psField.set(state, mockPersistenceService);
    }

    @Nested
    @DisplayName("Initial State & Basic Transitions")
    class InitialStateTests {

        @Test
        @DisplayName("executeState() with no args transitions START to SELECT_BID")
        void executeStateNoArgs() {
            StateStep step = state.executeState();
            assertTrue(step.result() instanceof BidSelectionResult);

            // Second call uses nextStep() directly
            StateStep step2 = state.executeState();
            assertTrue(step2.result() instanceof BidSelectionResult);
        }

        @Test
        @DisplayName("executeState(Command) at START phase overrides and transitions to SELECT_BID")
        void executeStateWithCommandAtStart() {
            StateStep step = state.executeState(new BidCommand(BidType.PASS));
            assertTrue(step.result() instanceof BidSelectionResult);
        }

        @Test
        @DisplayName("Throws on unhandled but permitted GameCommands")
        void throwsOnUnhandledCommand() {
            state.executeState(); // Move out of START phase

            // FIX: Use a real, permitted command that the switch doesn't expect!
            assertThrows(IllegalStateException.class, () -> state.executeState(new StartGameCommand()));
        }
    }

    @Nested
    @DisplayName("Normal Scoring Workflow (SOLO)")
    class NormalScoringWorkflow {

        @Test
        @DisplayName("Walks through SELECT_BID -> SELECT_TRUMP -> SELECT_PLAYERS -> CALCULATE -> SCOREBOARD")
        void walkSoloWorkflow() {
            state.executeState(); // START -> SELECT_BID

            // 1. Select Bid
            StateStep step1 = state.executeState(new BidCommand(BidType.SOLO));
            assertTrue(step1.result() instanceof SuitSelectionResult);

            // 2. Select Trump
            StateStep step2 = state.executeState(new SuitCommand(Suit.HEARTS));
            assertTrue(step2.result() instanceof PlayerSelectionResult);
            assertFalse(((PlayerSelectionResult) step2.result()).multiSelect(), "Solo should not be multi-select");

            // 3. Try Empty Player List (Should stay in phase and reprompt)
            StateStep step3Empty = state.executeState(new PlayerListCommand(Collections.emptyList()));
            assertTrue(step3Empty.result() instanceof PlayerSelectionResult);

            // 4. Select Player
            StateStep step3 = state.executeState(new PlayerListCommand(List.of(id1)));
            assertTrue(step3.result() instanceof AmountOfTrickWonResult);

            // 5. Input Tricks Won (Calculates Score)
            StateStep step4 = state.executeState(new NumberCommand(13));
            assertTrue(step4.result() instanceof ScoreBoardResult);

            // Verify Game integration
            verify(game).addRound(any(Round.class));
        }

        @Test
        @DisplayName("Suit Selection correctly flags multi-select for Team bids")
        void multiSelectForProposals() {
            state.executeState(); // START -> SELECT_BID
            state.executeState(new BidCommand(BidType.PROPOSAL));

            StateStep step = state.executeState(new SuitCommand(Suit.SPADES));
            assertTrue(((PlayerSelectionResult) step.result()).multiSelect(), "Proposal requires multi-select");
        }
    }

    @Nested
    @DisplayName("Miserie Scoring Workflow")
    class MiserieScoringWorkflow {

        @Test
        @DisplayName("Walks through SELECT_BID -> SELECT_PLAYERS -> SELECT_WINNERS -> SCOREBOARD")
        void walkMiserieWorkflow() {
            state.executeState(); // START -> SELECT_BID

            // 1. Select Bid (Skips Trump)
            StateStep step1 = state.executeState(new BidCommand(BidType.MISERIE));
            assertTrue(step1.result() instanceof PlayerSelectionResult);
            assertTrue(((PlayerSelectionResult) step1.result()).multiSelect());

            // 2. Select Participating Players
            StateStep step2 = state.executeState(new PlayerListCommand(List.of(id1, id2)));
            assertTrue(step2.result() instanceof PlayerSelectionResult, "Prompts for winners");

            // 3. Select Winning Players (Executes Calculation directly without tricks input)
            StateStep step3 = state.executeState(new PlayerListCommand(List.of(id1)));
            assertTrue(step3.result() instanceof ScoreBoardResult);

            verify(game).addRound(any(Round.class));
        }
    }

    @Nested
    @DisplayName("Menu Navigation & Operations (PROMPT_NEXT_STATE)")
    class MenuOperations {

        @BeforeEach
        void pushToMenu() {
            state.executeState();
            state.executeState(new BidCommand(BidType.PASS));
            state.executeState(new SuitCommand(Suit.HEARTS));
            state.executeState(new PlayerListCommand(List.of(id1)));
            state.executeState(new NumberCommand(0)); // Calculates and puts us in PROMPT_NEXT_STATE
        }

        @Test
        @DisplayName("Option 1 & 2: Set Next State Decision")
        void testStateTransitions() {
            StateStep step1 = state.executeState(new NumberCommand(1));
            assertTrue(step1.shouldTransition());
            assertTrue(state.nextState() instanceof CountState);

            StateStep step2 = state.executeState(new NumberCommand(2));
            assertTrue(step2.shouldTransition());
            assertNull(state.nextState());
        }

        @Test
        @DisplayName("Option 3: Save Description")
        void testSaveDescription() throws Exception {
            StateStep step = state.executeState(new NumberCommand(3));
            assertTrue(step.result() instanceof SaveDescriptionResult);

            StateStep saveStep = state.executeState(new TextCommand("Test Save"));
            assertTrue(saveStep.result() instanceof ScoreBoardResult);
            verify(mockPersistenceService).save(any(), any(), eq("Test Save"));
        }

        @Test
        @DisplayName("Option 3: Save Description throws correctly on DB failure")
        void testSaveDescriptionFailure() throws Exception {
            state.executeState(new NumberCommand(3));

            doThrow(new RuntimeException("DB offline")).when(mockPersistenceService).save(any(), any(), any());

            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> state.executeState(new TextCommand("Fail")));
            assertTrue(ex.getMessage().contains("Error in CountState handleSaveDescription"));
        }

        @Test
        @DisplayName("Option 4: Remove Round")
        void testRemoveRound() {
            when(game.getRounds()).thenReturn(List.of(mockRound));

            StateStep step = state.executeState(new NumberCommand(4));
            assertTrue(step.result() instanceof DeleteRoundResult);

            // Cancel removal
            StateStep cancelStep = state.executeState(new NumberCommand(0));
            assertTrue(cancelStep.result() instanceof ScoreBoardResult);

            // Actually remove round
            state.executeState(new NumberCommand(4));
            StateStep removeStep = state.executeState(new RoundCommand(mockRound));

            verify(game).removeRound(mockRound);
            verify(game).recalibrateScores();
            assertTrue(removeStep.result() instanceof ScoreBoardResult);
        }

        @Test
        @DisplayName("Option 5: Add Player")
        void testAddPlayer() {
            StateStep step = state.executeState(new NumberCommand(5));
            assertTrue(step.result() instanceof AddHumanPlayerResult);

            // Valid input adds
            StateStep addStep = state.executeState(new TextCommand("Eve"));
            verify(game).addPlayer(argThat(p -> p.getName().equals("Eve")));
            assertTrue(addStep.result() instanceof ScoreBoardResult);
        }

        @Test
        @DisplayName("Option 6: Remove Player")
        void testRemovePlayer() {
            // Cannot remove if <= 4 players
            when(game.getTotalPlayerCount()).thenReturn(4);
            StateStep rejectStep = state.executeState(new NumberCommand(6));
            assertTrue(rejectStep.result() instanceof ScoreBoardResult);

            // Can remove if > 4 players
            when(game.getTotalPlayerCount()).thenReturn(5);
            StateStep step = state.executeState(new NumberCommand(6));
            assertTrue(step.result() instanceof PlayerSelectionResult);

            // Empty input reprompts
            StateStep emptyStep = state.executeState(new PlayerListCommand(Collections.emptyList()));
            assertTrue(emptyStep.result() instanceof PlayerSelectionResult);

            // Valid input removes
            StateStep removeStep = state.executeState(new PlayerListCommand(List.of(id5)));
            verify(game).removePlayer(p5);
            assertTrue(removeStep.result() instanceof ScoreBoardResult);
        }

        @Test
        @DisplayName("Throws on invalid Number Inputs")
        void throwsOnInvalidNumbers() {
            assertThrows(IllegalStateException.class, () -> state.executeState(new NumberCommand(99)));
        }

        @Test
        @DisplayName("Throws on rogue Text Inputs out of phase")
        void throwsOnRogueTextInput() {
            assertThrows(IllegalStateException.class, () -> state.executeState(new TextCommand("Rogue")));
        }
    }
}