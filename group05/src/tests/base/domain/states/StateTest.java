package base.domain.states;

import base.domain.WhistGame;
import base.domain.commands.GameCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("State Base Class & Architecture")
class StateTest {

    @Mock private WhistGame mockGame;
    @Mock private State mockNextState;
    @Mock private StateStep mockStateStep;

    // FIX: Use a real, concrete record since GameCommand is sealed and cannot be mocked!
    private final GameCommand testCommand = new GameCommand.StartGameCommand();

    /**
     * A concrete implementation of the abstract State class strictly for testing base logic.
     */
    private class TestableState extends State {
        public TestableState(WhistGame game) {
            super(game);
        }

        @Override
        public State nextState() {
            return mockNextState;
        }

        @Override
        public StateStep executeState() {
            return mockStateStep;
        }

        @Override
        public StateStep executeState(GameCommand action) {
            return mockStateStep;
        }
    }

    @Nested
    @DisplayName("Constructor & Encapsulation")
    class InitializationTests {

        @Test
        @DisplayName("Successfully creates state and encapsulates WhistGame reference")
        void successfulInitialization() {
            TestableState state = new TestableState(mockGame);

            assertNotNull(state, "State should be successfully instantiated.");
            assertEquals(mockGame, state.getGame(), "getGame() must return the exact injected WhistGame instance.");
        }

        @Test
        @DisplayName("Rejects null WhistGame via constructor guard")
        void throwsOnNullGame() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> new TestableState(null),
                    "Constructor must throw IllegalArgumentException when game is null"
            );

            assertTrue(exception.getMessage().contains("WhistGame cannot be null"),
                    "Exception message should clearly indicate the null constraint.");
        }
    }

    @Nested
    @DisplayName("Abstract Method Contracts")
    class AbstractMethodTests {

        @Test
        @DisplayName("Abstract methods are properly callable through subclasses")
        void verifyAbstractSignatures() {
            TestableState state = new TestableState(mockGame);

            // While the base class has no bytecode for these methods,
            // invoking them on the testable class ensures the API contract is sound.
            assertEquals(mockNextState, state.nextState(), "nextState() contract is valid");
            assertEquals(mockStateStep, state.executeState(), "executeState() contract is valid");

            // FIX: Pass the real testCommand instead of a mock
            assertEquals(mockStateStep, state.executeState(testCommand), "executeState(GameCommand) contract is valid");
        }
    }
}