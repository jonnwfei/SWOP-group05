package base.domain.commands;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Start Game Command Tests")
class StartGameCommandTest {

    @Test
    @DisplayName("Should be successfully instantiated")
    void shouldInstantiate() {
        // Act
        StartGameCommand command = new StartGameCommand();

        // Assert
        assertThat(command).isNotNull();
    }

    @Test
    @DisplayName("Equality: All instances of an empty command should be equal")
    void shouldEvaluateEqualityByValue() {
        // Arrange
        StartGameCommand cmd1 = new StartGameCommand();
        StartGameCommand cmd2 = new StartGameCommand();

        // Assert
        assertThat(cmd1)
                .isEqualTo(cmd2)
                .hasSameHashCodeAs(cmd2);
    }

    @Test
    @DisplayName("toString should reflect the command name in standard record format")
    void shouldHaveDescriptiveToString() {
        // Arrange
        StartGameCommand command = new StartGameCommand();

        // Assert
        assertThat(command.toString()).isEqualTo("StartGameCommand[]");
    }
}