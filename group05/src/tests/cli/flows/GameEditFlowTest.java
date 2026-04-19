package cli.flows;

import base.domain.WhistGame;
import base.domain.player.Player;
import base.domain.round.Round;
import base.storage.GamePersistenceService;
import base.storage.snapshots.SaveMode;
import cli.TerminalManager;
import cli.elements.Response; // Use the real class!
import cli.events.CountEvents.PlayerSelectionIOEvent;
import cli.events.CountEvents.SaveDescriptionIOEvent;
import cli.events.MenuEvents.AddHumanPlayerIOEvent;
import cli.events.MenuEvents.AddPlayerIOEvent;
import cli.events.MenuEvents.DeleteRoundIOEvent;
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
@DisplayName("GameEditFlow Cross-Cutting Actions")
class GameEditFlowTest {

    @Mock private TerminalManager terminalManager;
    @Mock private WhistGame game;
    @Mock private GamePersistenceService persistenceService;

    private GameEditFlow flow;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
        flow = new GameEditFlow(terminalManager, game, persistenceService, SaveMode.GAME);
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    // FIX: Return a REAL Response object instead of a mock.
    // This solves the UnfinishedStubbingException and the "stubbing final method" warning.
    private Response realResponse(String input) {
        return new Response(input);
    }

    @Nested
    @DisplayName("Constructor & Mode Settings")
    class InitializationTests {
        @Test
        void constructorGuards() {
            assertThrows(IllegalArgumentException.class, () -> new GameEditFlow(null, game, persistenceService, SaveMode.GAME));
            assertThrows(IllegalArgumentException.class, () -> new GameEditFlow(terminalManager, null, persistenceService, SaveMode.GAME));
            assertThrows(IllegalArgumentException.class, () -> new GameEditFlow(terminalManager, game, null, SaveMode.GAME));
            assertThrows(IllegalArgumentException.class, () -> new GameEditFlow(terminalManager, game, persistenceService, null));
        }

        @Test
        void setModeTests() {
            assertDoesNotThrow(() -> flow.setMode(SaveMode.COUNT));
            assertThrows(IllegalArgumentException.class, () -> flow.setMode(null));
        }
    }

    @Nested
    @DisplayName("Save Game Workflow")
    class SaveGameTests {
        @Test
        void successfulSave() {
            when(terminalManager.handle(any(SaveDescriptionIOEvent.class)))
                    .thenReturn(realResponse(null), realResponse("   "), realResponse("My Save"));

            flow.saveGame();
            verify(persistenceService).save(game, SaveMode.GAME, "My Save");
        }

        @Test
        void handlesPersistenceErrors() {
            when(terminalManager.handle(any(SaveDescriptionIOEvent.class)))
                    .thenReturn(realResponse("Bad Save"));

            doThrow(new RuntimeException("Database offline"))
                    .when(persistenceService).save(any(), any(), anyString());

            assertDoesNotThrow(() -> flow.saveGame());
            assertTrue(outContent.toString().contains("Save failed: Database offline"));
        }
    }

    @Nested
    @DisplayName("Add Player Workflow")
    class AddPlayerTests {
        @Test
        void countModeForcesHuman() {
            flow.setMode(SaveMode.COUNT);
            when(terminalManager.handle(any(AddHumanPlayerIOEvent.class))).thenReturn(realResponse("Alice"));

            flow.addPlayer();
            verify(game).addPlayer(argThat(p -> p.getName().equals("Alice")));
        }

        @Test
        void gameModeAddsHuman() {
            when(terminalManager.handle(any(AddPlayerIOEvent.class)))
                    .thenReturn(realResponse("abc"), realResponse("9"), realResponse("1"));

            when(terminalManager.handle(any(AddHumanPlayerIOEvent.class)))
                    .thenReturn(realResponse("Bob"));

            flow.addPlayer();
            verify(game).addPlayer(argThat(p -> p.getName().equals("Bob")));
        }

        @Test
        void gameModeAddsSmartBot() {
            when(terminalManager.handle(any(AddPlayerIOEvent.class))).thenReturn(realResponse("2"));
            flow.addPlayer();
            verify(game).addPlayer(argThat(p -> p.getName().contains("Smart")));
        }

        @Test
        void gameModeAddsHighBot() {
            when(terminalManager.handle(any(AddPlayerIOEvent.class))).thenReturn(realResponse("3"));
            flow.addPlayer();
            verify(game).addPlayer(argThat(p -> p.getName().contains("High")));
        }

        @Test
        void gameModeAddsLowBot() {
            when(terminalManager.handle(any(AddPlayerIOEvent.class))).thenReturn(realResponse("4"));
            flow.addPlayer();
            verify(game).addPlayer(argThat(p -> p.getName().contains("Low")));
        }
    }

    @Nested
    @DisplayName("Remove Player Workflow")
    class RemovePlayerTests {
        @Mock private Player p1, p2;

        @BeforeEach
        void setupPlayers() {
            lenient().when(game.getAllPlayers()).thenReturn(List.of(p1, p2));
        }

        @Test
        void cannotRemove() {
            when(game.canRemovePlayer()).thenReturn(false);
            assertFalse(flow.removePlayer());
        }

        @Test
        void emptyInputCancels() {
            when(game.canRemovePlayer()).thenReturn(true);
            when(terminalManager.handle(any(PlayerSelectionIOEvent.class))).thenReturn(realResponse("   "));

            assertFalse(flow.removePlayer());
        }

        @Test
        void outOfBoundsIndex() {
            when(game.canRemovePlayer()).thenReturn(true);
            when(terminalManager.handle(any(PlayerSelectionIOEvent.class))).thenReturn(realResponse("0"));
            assertFalse(flow.removePlayer());

            when(terminalManager.handle(any(PlayerSelectionIOEvent.class))).thenReturn(realResponse("99"));
            assertFalse(flow.removePlayer());
        }

        @Test
        void successfulRemoval() {
            when(game.canRemovePlayer()).thenReturn(true);
            when(terminalManager.handle(any(PlayerSelectionIOEvent.class)))
                    .thenReturn(realResponse("abc"), realResponse("  1 ,, 2 "));

            assertTrue(flow.removePlayer());
            verify(game).removePlayer(p1);
        }
    }

    @Nested
    @DisplayName("Remove Round Workflow")
    class RemoveRoundTests {
        @Mock private Round mockRound1, mockRound2;

        @Test
        void noRoundsAvailable() {
            when(game.getRounds()).thenReturn(List.of());
            assertFalse(flow.removeRound());
        }

        @Test
        void userCancels() {
            when(game.getRounds()).thenReturn(List.of(mockRound1));
            when(terminalManager.handle(any(DeleteRoundIOEvent.class))).thenReturn(realResponse("0"));
            assertFalse(flow.removeRound());
        }

        @Test
        void successfulRoundRemoval() {
            when(game.getRounds()).thenReturn(List.of(mockRound1, mockRound2));
            when(terminalManager.handle(any(DeleteRoundIOEvent.class))).thenReturn(realResponse("2"));

            assertTrue(flow.removeRound());
            verify(game).removeRound(mockRound2);
            verify(game).recalibrateScores();
        }
    }
}