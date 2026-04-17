package base.domain.commands;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Number Command Tests")
class NumberCommandTest {

    @Nested
    @DisplayName("Constructor & Validation")
    class ConstructorTests {

        @ParameterizedTest(name = "Should allow valid choice: {0}")
        @ValueSource(ints = {0, 1, 13, 100})
        void shouldAcceptPositiveNumbers(int validChoice) {
            // Act
            NumberCommand command = new NumberCommand(validChoice);

            // Assert
            assertEquals(validChoice, command.choice());
        }

        @ParameterizedTest(name = "Should reject negative choice: {0}")
        @ValueSource(ints = {-1, -100})
        void shouldRejectNegativeNumbers(int invalidChoice) {
            // Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new NumberCommand(invalidChoice));
            assertTrue(exception.getMessage().contains("choice must be positive"));
        }
    }

    @Nested
    @DisplayName("Record Behavior")
    class RecordBehaviorTests {

        @Test
        @DisplayName("Equality should be based on the choice value")
        void shouldEvaluateEqualityByValue() {
            NumberCommand cmd1 = new NumberCommand(5);
            NumberCommand cmd2 = new NumberCommand(5);
            NumberCommand cmd3 = new NumberCommand(10);

            // Assert
            assertEquals(cmd1, cmd2);
            assertEquals(cmd1.hashCode(), cmd2.hashCode());
            assertNotEquals(cmd1, cmd3);
        }

        @Test
        @DisplayName("toString should clearly show the choice value")
        void shouldHaveDescriptiveToString() {
            NumberCommand command = new NumberCommand(42);
            String commandString = command.toString();

            // Assert
            assertTrue(commandString.contains("NumberCommand"));
            assertTrue(commandString.contains("choice=42"));
        }
    }
}