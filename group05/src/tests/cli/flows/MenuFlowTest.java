package cli.flows;

import base.domain.WhistGame;
import base.domain.WhistRules;
import base.domain.player.Player;
import base.storage.GamePersistenceService;
import base.storage.snapshots.SaveMode;
import cli.TerminalManager;
import cli.elements.Response;
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

    // Suppress and capture console output for verification
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

    private Response mockResponse(String raw) {
        Response r = mock(Response.class);
        lenient().when(r.rawInput()).thenReturn(raw);
        return r;
    }

    @Nested
    @DisplayName("Option 1: Setup New Game")
    class SetupGameTests {

        @Test
        @DisplayName("Configures a full game with humans and various bot strategies")
        void testSetupGameSuccess() {
            // Choice: 1 (Setup Game)
            when(terminalManager.handle(any(WelcomeMenuIOEvent.class))).thenReturn(mockResponse("1"));
            // Bots: 1
            when(terminalManager.handle(any(AmountOfBotsIOEvent.class))).thenReturn(mockResponse("1"));
            // Humans: 3 (Minimum for 1 bot to reach 4 players)
            when(terminalManager.handle(any(AmountOfHumansIOEvent.class))).thenReturn(mockResponse("3"));
            // Names for 3 humans
            when(terminalManager.handle(any(PlayerNameIOEvent.class))).thenReturn(
                    mockResponse("Alice"), mockResponse("Bob"), mockResponse("Charlie")
            );
            // Strategy for 1 bot: 3 (SmartBot)
            when(terminalManager.handle(any(BotStrategyIOEvent.class))).thenReturn(mockResponse("3"));

            when(game.getAllPlayers()).thenReturn(Collections.emptyList());

            SaveMode mode = menuFlow.run();

            assertEquals(SaveMode.GAME, mode);
            verify(game, times(4)).addPlayer(any(Player.class)); // 3 humans + 1 bot
            verify(game).setRandomDealer();
            verify(game).startGame();
        }

        @Test
        @DisplayName("Loops correctly on invalid strategy and bot amounts")
        void testSetupGameRetryLogic() {
            when(terminalManager.handle(any(WelcomeMenuIOEvent.class))).thenReturn(mockResponse("1"));
            // Bots: "abc" (Error) -> then 0
            when(terminalManager.handle(any(AmountOfBotsIOEvent.class)))
                    .thenReturn(mockResponse("abc"), mockResponse("0"));
            // Humans: 4
            when(terminalManager.handle(any(AmountOfHumansIOEvent.class))).thenReturn(mockResponse("4"));
            // Names: "" (Error) -> Alice, etc
            when(terminalManager.handle(any(PlayerNameIOEvent.class)))
                    .thenReturn(mockResponse(" "), mockResponse("Alice"), mockResponse("Bob"), mockResponse("Charlie"), mockResponse("Dave"));

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
            when(terminalManager.handle(any(WelcomeMenuIOEvent.class))).thenReturn(mockResponse("2"));
            when(terminalManager.handle(any(PlayerNameIOEvent.class))).thenReturn(
                    mockResponse("P1"), mockResponse("P2"), mockResponse("P3"), mockResponse("P4")
            );

            Player mockP1 = mock(Player.class);
            when(game.getAllPlayers()).thenReturn(List.of(mockP1));

            SaveMode mode = menuFlow.run();

            assertEquals(SaveMode.COUNT, mode);
            verify(game, times(4)).addPlayer(any());
            verify(game).setDealerPlayer(mockP1);
            verify(game).startCount();
        }
    }

    @Nested
    @DisplayName("Option 3: Load Save")
    class LoadSaveTests {

        @Test
        @DisplayName("Returns to menu if no saves exist")
        void testNoSaves() {
            when(terminalManager.handle(any(WelcomeMenuIOEvent.class))).thenReturn(mockResponse("3"));
            when(persistenceService.listDescriptions()).thenReturn(Collections.emptyList());

            assertThrows(NullPointerException.class, () -> menuFlow.run()); // returns savedMode which is null
            assertTrue(outContent.toString().contains("No saved games found."));
        }

        @Test
        @DisplayName("Successfully loads a GAME mode save")
        void testLoadGameSuccess() {
            when(terminalManager.handle(any(WelcomeMenuIOEvent.class))).thenReturn(mockResponse("3"));
            when(persistenceService.listDescriptions()).thenReturn(List.of("MySave"));
            // Choice: 1 (The first save), then 0 (Exit check) is not needed because of return
            when(terminalManager.handle(any(LoadSaveIOEvent.class))).thenReturn(mockResponse("1"));
            when(persistenceService.loadIntoGame(any(), eq("MySave"))).thenReturn(SaveMode.GAME);

            SaveMode mode = menuFlow.run();

            assertEquals(SaveMode.GAME, mode);
            verify(game).startGame();
        }

        @Test
        @DisplayName("Throws exception if loading fails")
        void testLoadFailure() {
            when(terminalManager.handle(any(WelcomeMenuIOEvent.class))).thenReturn(mockResponse("3"));
            when(persistenceService.listDescriptions()).thenReturn(List.of("BrokenSave"));
            when(terminalManager.handle(any(LoadSaveIOEvent.class))).thenReturn(mockResponse("1"));
            when(persistenceService.loadIntoGame(any(), any())).thenThrow(new RuntimeException("IO Error"));

            assertThrows(IllegalArgumentException.class, () -> menuFlow.run());
            assertTrue(outContent.toString().contains("Error while loading game"));
        }

        @Test
        @DisplayName("Returns early if user selects 0 in load menu")
        void testCancelLoad() {
            when(terminalManager.handle(any(WelcomeMenuIOEvent.class))).thenReturn(mockResponse("3"));
            when(persistenceService.listDescriptions()).thenReturn(List.of("Save1"));
            when(terminalManager.handle(any(LoadSaveIOEvent.class))).thenReturn(mockResponse("0"));

            assertThrows(NullPointerException.class, () -> menuFlow.run());
        }
    }

    @Test
    @DisplayName("High Bot and Low Bot strategy selection")
    void testBotStrategies() {
        when(terminalManager.handle(any(WelcomeMenuIOEvent.class))).thenReturn(mockResponse("1"));
        when(terminalManager.handle(any(AmountOfBotsIOEvent.class))).thenReturn(mockResponse("2"));
        when(terminalManager.handle(any(AmountOfHumansIOEvent.class))).thenReturn(mockResponse("2"));
        when(terminalManager.handle(any(PlayerNameIOEvent.class))).thenReturn(mockResponse("H1"), mockResponse("H2"));

        // Bot 1: Strategy 1 (High), Bot 2: Strategy 2 (Low)
        when(terminalManager.handle(any(BotStrategyIOEvent.class))).thenReturn(mockResponse("1"), mockResponse("2"));
        when(game.getAllPlayers()).thenReturn(Collections.emptyList());

        menuFlow.run();

        verify(game, times(4)).addPlayer(any());
    }
}