package cli.flows;

import base.domain.WhistGame;
import base.domain.player.Player;
import base.storage.snapshots.SaveMode;
import cli.TerminalManager;
import cli.elements.Response; // Use the real class!
import cli.events.CountEvents.ScoreBoardIOEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScoreBoardFlow Logic & Routing")
class ScoreBoardFlowTest {

    @Mock private TerminalManager terminalManager;
    @Mock private WhistGame game;
    @Mock private GameEditFlow editFlow;
    @Mock private Player p1, p2, p3, p4, p5;

    private ScoreBoardFlow scoreBoardFlow;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
        scoreBoardFlow = new ScoreBoardFlow(terminalManager, game, editFlow);

        // Standard setup for player lists to prevent NPEs in showMenu
        lenient().when(game.getAllPlayers()).thenReturn(List.of(p1, p2, p3, p4));
        lenient().when(p1.getName()).thenReturn("Alice");
        lenient().when(p1.getScore()).thenReturn(10);
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    // FIX: Return a REAL Response object instead of a mock.
    // This prevents the UnfinishedStubbingException.
    private Response realResponse(String raw) {
        return new Response(raw);
    }

    @Nested
    @DisplayName("Constructor Guards")
    class ConstructorTests {
        @Test
        @DisplayName("Throws exception if any dependency is null")
        void testNullGuards() {
            assertThrows(IllegalArgumentException.class, () -> new ScoreBoardFlow(null, game, editFlow));
            assertThrows(IllegalArgumentException.class, () -> new ScoreBoardFlow(terminalManager, null, editFlow));
            assertThrows(IllegalArgumentException.class, () -> new ScoreBoardFlow(terminalManager, game, null));
        }
    }

    @Nested
    @DisplayName("Option 1: Continue Game/Count")
    class ProgressionTests {

        @Test
        @DisplayName("SaveMode.GAME with 4 players: advances dealer and starts game")
        void testContinueGameStandard() {
            when(terminalManager.handle(any(ScoreBoardIOEvent.class))).thenReturn(realResponse("1"));
            when(game.getAllPlayers()).thenReturn(List.of(p1, p2, p3, p4));

            boolean result = scoreBoardFlow.run(SaveMode.GAME);

            assertTrue(result);
            verify(game, never()).rotateActivePlayers();
            verify(game).startGame();
            verify(game).advanceDealer();
        }

        @Test
        @DisplayName("SaveMode.GAME with >4 players: rotates players, advances dealer and starts game")
        void testContinueGameWithRotation() {
            when(terminalManager.handle(any(ScoreBoardIOEvent.class))).thenReturn(realResponse("1"));
            when(game.getAllPlayers()).thenReturn(List.of(p1, p2, p3, p4, p5));

            boolean result = scoreBoardFlow.run(SaveMode.GAME);

            assertTrue(result);
            verify(game).rotateActivePlayers();
            verify(game).startGame();
            verify(game).advanceDealer();
        }

        @Test
        @DisplayName("SaveMode.COUNT: starts count mode")
        void testContinueCount() {
            when(terminalManager.handle(any(ScoreBoardIOEvent.class))).thenReturn(realResponse("1"));

            boolean result = scoreBoardFlow.run(SaveMode.COUNT);

            assertTrue(result);
            verify(game).startCount();
            verify(game, never()).advanceDealer();
        }
    }

    @Nested
    @DisplayName("Option 2: Exit to Main Menu")
    class ExitTests {
        @Test
        @DisplayName("Returns false to signal exit to the controller")
        void testExit() {
            when(terminalManager.handle(any(ScoreBoardIOEvent.class))).thenReturn(realResponse("2"));
            assertFalse(scoreBoardFlow.run(SaveMode.GAME));
        }
    }

    @Nested
    @DisplayName("Edit Actions Delegation")
    class EditActionTests {

        @Test
        @DisplayName("Delegates save, remove round, and add player then returns to loop")
        void testDelegationLoop() {
            // Sequence: 3 (Save), 4 (Remove Round), 5 (Add Player), 2 (Exit)
            when(terminalManager.handle(any(ScoreBoardIOEvent.class))).thenReturn(
                    realResponse("3"), realResponse("4"), realResponse("5"), realResponse("2")
            );

            scoreBoardFlow.run(SaveMode.GAME);

            verify(editFlow).saveGame();
            verify(editFlow).removeRound();
            verify(editFlow).addPlayer();
        }

        @Test
        @DisplayName("Option 6: Remove Player (when permitted)")
        void testRemovePlayerDelegation() {
            when(game.canRemovePlayer()).thenReturn(true);
            when(terminalManager.handle(any(ScoreBoardIOEvent.class))).thenReturn(
                    realResponse("6"), realResponse("2")
            );

            scoreBoardFlow.run(SaveMode.GAME);
            verify(editFlow).removePlayer();
        }
    }

    @Nested
    @DisplayName("Input Validation & Errors")
    class InputValidationTests {

        @Test
        @DisplayName("Loops when input is out of range or not a number")
        void testAskIntRetryLogic() {
            // First: "abc" (Error), Second: "99" (Out of range), Third: "2" (Valid)
            when(terminalManager.handle(any(ScoreBoardIOEvent.class))).thenReturn(
                    realResponse("abc"), realResponse("99"), realResponse("2")
            );

            scoreBoardFlow.run(SaveMode.GAME);

            String output = outContent.toString();
            assertTrue(output.contains("Invalid input. Please enter a number."));
            assertTrue(output.contains("Please enter a number between 1 and 5."));
        }

        @Test
        @DisplayName("Enforces range based on player removal eligibility")
        void testRangeEnforcement() {
            when(game.canRemovePlayer()).thenReturn(true); // max should be 6
            when(terminalManager.handle(any(ScoreBoardIOEvent.class))).thenReturn(
                    realResponse("7"), realResponse("2")
            );

            scoreBoardFlow.run(SaveMode.GAME);
            assertTrue(outContent.toString().contains("Please enter a number between 1 and 6."));
        }
    }
}