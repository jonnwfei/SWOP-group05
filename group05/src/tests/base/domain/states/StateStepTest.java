package base.domain.states;

import base.domain.results.BidResults;
import base.domain.results.GameResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StateStep Record & Factory Logic")
class StateStepTest {

    // FIX: Use a real, permitted concrete record since GameResult is a sealed interface and cannot be mocked!
    private final GameResult testResult = new BidResults.BiddingCompleted();

    @Nested
    @DisplayName("Static Factory Methods")
    class FactoryMethodTests {

        @Test
        @DisplayName("stay() creates a StateStep with a result and NO transition")
        void stay_CreatesCorrectStateStep() {
            StateStep step = StateStep.stay(testResult);

            assertEquals(testResult, step.result());
            assertFalse(step.shouldTransition(), "stay() should not trigger a transition");
        }

        @Test
        @DisplayName("stay() throws IllegalArgumentException if result is null")
        void stay_ThrowsOnNull() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> StateStep.stay(null));
            assertTrue(exception.getMessage().contains("Result must not be null"));
        }

        @Test
        @DisplayName("transition() creates a StateStep with a result AND a transition")
        void transition_CreatesCorrectStateStep() {
            StateStep step = StateStep.transition(testResult);

            assertEquals(testResult, step.result());
            assertTrue(step.shouldTransition(), "transition() should trigger a transition");
        }

        @Test
        @DisplayName("transition() throws IllegalArgumentException if result is null")
        void transition_ThrowsOnNull() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> StateStep.transition(null));
            assertTrue(exception.getMessage().contains("Result must not be null"));
        }

        @Test
        @DisplayName("transitionWithoutResult() creates a StateStep with NO result AND a transition")
        void transitionWithoutResult_CreatesCorrectStateStep() {
            StateStep step = StateStep.transitionWithoutResult();

            assertNull(step.result(), "transitionWithoutResult() should have a null result");
            assertTrue(step.shouldTransition(), "transitionWithoutResult() should trigger a transition");
        }
    }

    @Nested
    @DisplayName("Instance Methods")
    class InstanceMethodTests {

        @Test
        @DisplayName("hasResult() returns true when a result exists")
        void hasResult_ReturnsTrueWhenNotNull() {
            StateStep step = StateStep.stay(testResult);
            assertTrue(step.hasResult());
        }

        @Test
        @DisplayName("hasResult() returns false when result is null")
        void hasResult_ReturnsFalseWhenNull() {
            StateStep step = StateStep.transitionWithoutResult();
            assertFalse(step.hasResult());

            // Testing native constructor directly just to ensure robust coverage
            StateStep nativeNullStep = new StateStep(null, false);
            assertFalse(nativeNullStep.hasResult());
        }
    }

    @Nested
    @DisplayName("Implicit Record Methods")
    class RecordImplicitMethodsTests {

        @Test
        @DisplayName("Validates implicit equals() and hashCode() methods")
        void equalsAndHashCode() {
            StateStep step1 = StateStep.stay(testResult);
            StateStep step2 = StateStep.stay(testResult);
            StateStep stepDifferentResult = StateStep.transitionWithoutResult();
            StateStep stepDifferentTransition = StateStep.transition(testResult);

            // Same state
            assertEquals(step1, step2);
            assertEquals(step1.hashCode(), step2.hashCode());

            // Different state
            assertNotEquals(step1, stepDifferentResult);
            assertNotEquals(step1, stepDifferentTransition);

            // Edge cases
            assertNotEquals(step1, null);
            assertNotEquals(step1, new Object());
        }

        @Test
        @DisplayName("Validates implicit toString() method")
        void testToString() {
            StateStep step = StateStep.stay(testResult);
            String stringRepresentation = step.toString();

            assertTrue(stringRepresentation.contains("StateStep"));
            assertTrue(stringRepresentation.contains("shouldTransition=false"));
            assertTrue(stringRepresentation.contains("result="));
        }
    }
}