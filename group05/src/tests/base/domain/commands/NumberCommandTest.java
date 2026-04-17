package base.domain.commands;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
            assertThat(command.choice()).isEqualTo(validChoice);
        }

        @ParameterizedTest(name = "Should reject negative choice: {0}")
        @ValueSource(ints = {-1, -100})
        void shouldRejectNegativeNumbers(int invalidChoice) {
            // Assert
            assertThatThrownBy(() -> new NumberCommand(invalidChoice))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("choice must be positive");
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

            assertThat(cmd1)
                    .isEqualTo(cmd2)
                    .hasSameHashCodeAs(cmd2)
                    .isNotEqualTo(cmd3);
        }

        @Test
        @DisplayName("toString should clearly show the choice value")
        void shouldHaveDescriptiveToString() {
            NumberCommand command = new NumberCommand(42);

            assertThat(command.toString())
                    .contains("NumberCommand")
                    .contains("choice=42");
        }
    }
}