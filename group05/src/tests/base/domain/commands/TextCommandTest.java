package base.domain.commands;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
            assertThat(command.text()).isEqualTo("Save Game 1");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException if text is null")
        void shouldRejectNullText() {
            assertThatThrownBy(() -> new TextCommand(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null or blank");
        }

        @ParameterizedTest(name = "Should throw IllegalArgumentException for blank input: ''{0}''")
        @ValueSource(strings = {"", " ", "   ", "\n", "\t"})
        void shouldRejectBlankText(String invalidText) {
            assertThatThrownBy(() -> new TextCommand(invalidText))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null or blank");
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

            assertThat(cmd1)
                    .isEqualTo(cmd2)
                    .hasSameHashCodeAs(cmd2)
                    .isNotEqualTo(cmd3);
        }

        @Test
        @DisplayName("toString should contain the encapsulated text")
        void shouldHaveDescriptiveToString() {
            TextCommand command = new TextCommand("WhistRound1");

            assertThat(command.toString())
                    .contains("TextCommand")
                    .contains("WhistRound1");
        }
    }
}