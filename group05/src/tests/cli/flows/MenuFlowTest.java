package cli.flows;

import base.domain.WhistGame;
import base.domain.player.Player;
import base.storage.GamePersistenceService;
import base.storage.snapshots.SaveMode;
import cli.TerminalManager;
import cli.elements.Response; // Using the real class
import cli.events.MenuEvents.*;
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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MenuFlow Configuration & Setup")
class MenuFlowTest {

    @Mock private TerminalManager terminalManager;
    @Mock private GamePersistenceService persistenceService;
    @Mock private WhistGame game;

    private MenuFlow menuFlow;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
        menuFlow = new MenuFlow(terminalManager, persistenceService, game);
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    // FIX: Return a real Response object to avoid nested stubbing errors
    private Response realResponse(String raw) {
        return new Response(raw);
    }

    @Nested
    @DisplayName("Option 1: Setup New Game")
    class SetupGameTests {

        @Test
        @DisplayName("Configures a full game with humans and various bot strategies")
        void testSetupGameSuccess() {
            when(terminalManager.handle(any(WelcomeMenuIOEvent.class))).thenReturn(realResponse("1"));
            when(terminalManager.handle(any(AmountOfBotsIOEvent.class))).thenReturn(realResponse("1"));
            when(terminalManager.handle(any(AmountOfHumansIOEvent.class))).thenReturn(realResponse("3"));
            when(terminalManager.handle(any(PlayerNameIOEvent.class))).thenReturn(
                    realResponse("Alice"), realResponse("Bob"), realResponse("Charlie")
            );
            when(terminalManager.handle(any(BotStrategyIOEvent.class))).thenReturn(realResponse("3"));

            when(game.getAllPlayers()).thenReturn(Collections.emptyList());

            SaveMode mode = menuFlow.run();

            assertEquals(SaveMode.GAME, mode);
            verify(game, times(4)).addPlayer(any(Player.class));
            verify(game).startGame();
        }

        @Test
        @DisplayName("Loops correctly on invalid strategy and bot amounts")
        void testSetupGameRetryLogic() {
            when(terminalManager.handle(any(WelcomeMenuIOEvent.class))).thenReturn(realResponse("1"));
            when(terminalManager.handle(any(AmountOfBotsIOEvent.class)))
                    .thenReturn(realResponse("abc"), realResponse("0"));
            when(terminalManager.handle(any(AmountOfHumansIOEvent.class))).thenReturn(realResponse("4"));
            when(terminalManager.handle(any(PlayerNameIOEvent.class)))
                    .thenReturn(realResponse(" "), realResponse("Alice"), realResponse("Bob"), realResponse("Charlie"), realResponse("Dave"));

            menuFlow.run();

            assertTrue(outContent.toString().contains("Invalid input. Please enter a number."));
            assertTrue(outContent.toString().contains("Input cannot be empty."));
            verify(game, times(4)).addPlayer(any());
        }
    }

    @Nested
    @DisplayName("Option 2: Setup Count Mode")
    class SetupCountTests {

        @Test
        @DisplayName("Configures count mode with 4 human players")
        void testSetupCountSuccess() {
            when(terminalManager.handle(any(WelcomeMenuIOEvent.class))).thenReturn(realResponse("2"));
            when(terminalManager.handle(any(PlayerNameIOEvent.class))).thenReturn(
                    realResponse("P1"), realResponse("P2"), realResponse("P3"), realResponse("P4")
            );

            Player mockP1 = mock(Player.class);
            when(game.getAllPlayers()).thenReturn(List.of(mockP1));

            SaveMode mode = menuFlow.run();

            assertEquals(SaveMode.COUNT, mode);
            verify(game, times(4)).addPlayer(any());
            verify(game).startCount();
        }
    }

    @Nested
    @DisplayName("Option 3: Load Save")
    class LoadSaveTests {

        @Test
        @DisplayName("Successfully loads a GAME mode save")
        void testLoadGameSuccess() {
            when(terminalManager.handle(any(WelcomeMenuIOEvent.class))).thenReturn(realResponse("3"));
            when(persistenceService.listDescriptions()).thenReturn(List.of("MySave"));
            when(terminalManager.handle(any(LoadSaveIOEvent.class))).thenReturn(realResponse("1"));
            when(persistenceService.loadIntoGame(any(), eq("MySave"))).thenReturn(SaveMode.GAME);

            SaveMode mode = menuFlow.run();

            assertEquals(SaveMode.GAME, mode);
            verify(game).startGame();
        }

        @Test
        @DisplayName("Throws exception if loading fails")
        void testLoadFailure() {
            when(terminalManager.handle(any(WelcomeMenuIOEvent.class))).thenReturn(realResponse("3"));
            when(persistenceService.listDescriptions()).thenReturn(List.of("BrokenSave"));
            when(terminalManager.handle(any(LoadSaveIOEvent.class))).thenReturn(realResponse("1"));
            when(persistenceService.loadIntoGame(any(), any())).thenThrow(new RuntimeException("IO Error"));

            assertThrows(IllegalArgumentException.class, () -> menuFlow.run());
        }
    }

    @Test
    @DisplayName("High Bot and Low Bot strategy selection")
    void testBotStrategies() {
        when(terminalManager.handle(any(WelcomeMenuIOEvent.class))).thenReturn(realResponse("1"));
        when(terminalManager.handle(any(AmountOfBotsIOEvent.class))).thenReturn(realResponse("2"));
        when(terminalManager.handle(any(AmountOfHumansIOEvent.class))).thenReturn(realResponse("2"));
        when(terminalManager.handle(any(PlayerNameIOEvent.class))).thenReturn(realResponse("H1"), realResponse("H2"));

        when(terminalManager.handle(any(BotStrategyIOEvent.class))).thenReturn(realResponse("1"), realResponse("2"));
        when(game.getAllPlayers()).thenReturn(Collections.emptyList());

        menuFlow.run();
        verify(game, times(4)).addPlayer(any());
    }
}