package cli;

import base.GameController;
import base.domain.WhistGame;
import base.domain.commands.GameCommand;
import base.domain.results.CountResults;
import base.domain.results.GameResult;
import base.storage.GamePersistenceService;
import base.storage.snapshots.SaveMode;
import cli.adapter.Adapter;
import cli.adapter.AdapterResponse;
import cli.adapter.AdapterResult;
import cli.elements.Response;
import cli.events.BidEvents;
import cli.events.CountEvents;
import cli.events.IOEvent;
import cli.flows.GameEditFlow;
import cli.flows.MenuFlow;
import cli.flows.ScoreBoardFlow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameCli Architecture & Application Loops")
class GameCliTest {

    @Mock private GameController mockController;
    @Mock private TerminalManager mockTerminalManager;
    @Mock private Adapter mockAdapter;
    @Mock private MenuFlow mockMenuFlow;
    @Mock private GamePersistenceService mockPersistenceService;

    private GameCli cli;

    // Real instances of sealed classes to bypass Mockito proxy restrictions
    private final GameResult concreteResult = new CountResults.AmountOfTrickWonResult();
    private final GameCommand concreteCommand = new GameCommand.NumberCommand(1);

    @BeforeEach
    void setUp() throws Exception {
        // Instantiate the cli normally, then inject our mocks to override the hardcoded dependencies
        cli = new GameCli();

        setPrivateField(cli, "controller", mockController);
        setPrivateField(cli, "terminalManager", mockTerminalManager);
        setPrivateField(cli, "adapter", mockAdapter);
        setPrivateField(cli, "menuFlow", mockMenuFlow);
        setPrivateField(cli, "persistenceService", mockPersistenceService);
    }

    @Nested
    @DisplayName("Application Loop (run method)")
    class RunMethodTests {

        @Test
        @DisplayName("Executes flows and resets properly until the infinite loop is interrupted")
        void testMainLoop() {
            // Intercept the internal creations of the UI Flows to prevent real terminal rendering
            try (MockedConstruction<GameEditFlow> editMocks = mockConstruction(GameEditFlow.class);
                 MockedConstruction<ScoreBoardFlow> scoreMocks = mockConstruction(ScoreBoardFlow.class,
                         (mock, context) -> when(mock.run(any())).thenReturn(false))) { // Exit inner loop after 1 pass

                when(mockMenuFlow.run()).thenReturn(SaveMode.GAME);
                when(mockController.isGameOver()).thenReturn(true); // Skip the session loop completely

                // Deliberately crash the controller reset on the SECOND pass to escape the while(true) loop
                doNothing().doThrow(new RuntimeException("Break Infinite Loop")).when(mockController).reset();

                // Assert the loop ran and broke safely
                assertThrows(RuntimeException.class, () -> cli.run());

                // Verify the top-level loop ran exactly twice (crashing on the second entry)
                verify(mockController, times(2)).reset();
                verify(mockMenuFlow, times(1)).run();
            }
        }
    }

    @Nested
    @DisplayName("Session Loop (runSession method)")
    class SessionLoopTests {

        @Test
        @DisplayName("Exits immediately if game is over")
        void exitsIfGameOver() throws Exception {
            when(mockController.isGameOver()).thenReturn(true);
            when(mockController.advance(null)).thenReturn(concreteResult);

            invokePrivateMethod("runSession");

            verify(mockController, times(1)).isGameOver();
            verify(mockController, never()).advance(concreteCommand);
        }

        @Test
        @DisplayName("Processes Immediate results without pausing for terminal IO")
        void processesImmediateResult() throws Exception {
            when(mockController.isGameOver()).thenReturn(false, true);
            when(mockController.advance(null)).thenReturn(concreteResult);
            when(mockController.advance(concreteCommand)).thenReturn(concreteResult);

            // Adapter skips IO and instantly returns a domain command (Bot behavior / Fast-forwards)
            AdapterResult.Immediate immediate = new AdapterResult.Immediate(concreteCommand);
            when(mockAdapter.handleResult(concreteResult)).thenReturn(immediate);

            invokePrivateMethod("runSession");

            verify(mockAdapter).handleResult(concreteResult);
            verify(mockTerminalManager, never()).handle(any(IOEvent.class));
            verify(mockController).advance(concreteCommand);
        }

        @Test
        @DisplayName("Processes NeedsIO results and correctly delegates to handleIO")
        void processesNeedsIOResult() throws Exception {
            when(mockController.isGameOver()).thenReturn(false, true);
            when(mockController.advance(null)).thenReturn(concreteResult);
            when(mockController.advance(concreteCommand)).thenReturn(concreteResult);

            // Adapter requests UI rendering
            AdapterResult.NeedsIO needsIO = new AdapterResult.NeedsIO(Collections.emptyList(), null);
            when(mockAdapter.handleResult(concreteResult)).thenReturn(needsIO);

            // Mock the internal handleIO response sequence
            AdapterResponse mockAdapterResp = mock(AdapterResponse.class);
            when(mockAdapterResp.shouldReRenderLastResult()).thenReturn(false);
            when(mockAdapterResp.command()).thenReturn(concreteCommand);

            when(mockTerminalManager.handle((IOEvent) null)).thenReturn(null);
            when(mockAdapter.handleResponse(null, concreteResult)).thenReturn(mockAdapterResp);

            invokePrivateMethod("runSession");

            verify(mockController).advance(concreteCommand);
            verify(mockTerminalManager).handle((IOEvent) null);
        }
    }

    @Nested
    @DisplayName("Terminal Interaction (handleIO method)")
    class HandleIOTests {

        @Test
        @DisplayName("Renders preambles, delegates primary events, and loops on re-render requests")
        void fullIOEventLifecycle() throws Exception {
            IOEvent preEvent = new BidEvents.BiddingCompletedIOEvent();
            IOEvent mainEvent = new CountEvents.SaveDescriptionIOEvent();
            IOEvent immediateEvent = new CountEvents.TrickInputIOEvent();
            Response mockResp1 = mock(Response.class);
            Response mockResp2 = mock(Response.class);

            AdapterResult.NeedsIO needsIO = new AdapterResult.NeedsIO(List.of(preEvent), mainEvent);

            // 1. Preamble is rendered silently
            when(mockTerminalManager.handle(preEvent)).thenReturn(null);

            // 2. Main event is rendered, user responds twice
            when(mockTerminalManager.handle(mainEvent)).thenReturn(mockResp1, mockResp2);

            // 3. First response triggers a validation error (shouldReRenderLastResult = true)
            AdapterResponse resp1 = mock(AdapterResponse.class);
            when(resp1.shouldReRenderLastResult()).thenReturn(true);
            when(resp1.immediateEvents()).thenReturn(List.of(immediateEvent));

            // 4. Second response is successful
            AdapterResponse resp2 = mock(AdapterResponse.class);
            when(resp2.shouldReRenderLastResult()).thenReturn(false);
            when(resp2.command()).thenReturn(concreteCommand);

            when(mockAdapter.handleResponse(mockResp1, concreteResult)).thenReturn(resp1);
            when(mockAdapter.handleResponse(mockResp2, concreteResult)).thenReturn(resp2);

            when(mockTerminalManager.handle(immediateEvent)).thenReturn(null);

            // Execute private method
            GameCommand resultCommand = invokePrivateMethodArgs("handleIO",
                    new Class<?>[]{AdapterResult.NeedsIO.class, GameResult.class},
                    new Object[]{needsIO, concreteResult});

            // Assertions
            assertEquals(concreteCommand, resultCommand, "Must return the command from the final valid response");
            verify(mockTerminalManager).handle(preEvent);
            verify(mockTerminalManager, times(2)).handle(mainEvent); // Promoted once, rejected, reprompted
            verify(mockTerminalManager).handle(immediateEvent); // Error message rendered
        }
    }

    // =========================================================================
    // Reflection Helper Utilities
    // =========================================================================

    private void setPrivateField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    private void invokePrivateMethod(String methodName) throws Exception {
        Method method = cli.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        try {
            method.invoke(cli);
        } catch (Exception e) {
            if (e.getCause() instanceof Exception) throw (Exception) e.getCause();
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T invokePrivateMethodArgs(String methodName, Class<?>[] argTypes, Object[] args) throws Exception {
        Method method = cli.getClass().getDeclaredMethod(methodName, argTypes);
        method.setAccessible(true);
        try {
            return (T) method.invoke(cli, args);
        } catch (Exception e) {
            if (e.getCause() instanceof Exception) throw (Exception) e.getCause();
            throw e;
        }
    }
}
