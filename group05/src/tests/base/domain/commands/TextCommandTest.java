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

@DisplayName("Text Command Tests")
class TextCommandTest {

    @Nested
    @DisplayName("Constructor & Validation")
    class ConstructorTests {

        @Test
        @DisplayName("Should successfully create command with a valid string")
        void shouldAcceptValidText() {
            // Act
            TextCommand command = new TextCommand("Save Game 1");

            // Assert
            assertEquals("Save Game 1", command.text());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException if text is null")
        void shouldRejectNullText() {
            // Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new TextCommand(null));
            assertTrue(exception.getMessage().contains("cannot be null or blank"));
        }

        @ParameterizedTest(name = "Should throw IllegalArgumentException for blank input: ''{0}''")
        @ValueSource(strings = {"", " ", "   ", "\n", "\t"})
        void shouldRejectBlankText(String invalidText) {
            // Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new TextCommand(invalidText));
            assertTrue(exception.getMessage().contains("cannot be null or blank"));
        }
    }

    @Nested
    @DisplayName("Record Behavior")
    class RecordBehaviorTests {

        @Test
        @DisplayName("Equality should be case-sensitive and value-based")
        void shouldEvaluateEqualityByValue() {
            TextCommand cmd1 = new TextCommand("Alpha");
            TextCommand cmd2 = new TextCommand("Alpha");
            TextCommand cmd3 = new TextCommand("alpha"); // Different case

            // Assert
            assertEquals(cmd1, cmd2);
            assertEquals(cmd1.hashCode(), cmd2.hashCode());
            assertNotEquals(cmd1, cmd3);
        }

        @Test
        @DisplayName("toString should contain the encapsulated text")
        void shouldHaveDescriptiveToString() {
            TextCommand command = new TextCommand("WhistRound1");
            String commandString = command.toString();

            // Assert
            assertTrue(commandString.contains("TextCommand"));
            assertTrue(commandString.contains("WhistRound1"));
        }
    }
}