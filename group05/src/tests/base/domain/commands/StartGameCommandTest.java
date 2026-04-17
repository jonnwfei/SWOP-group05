package base.domain.commands;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Start Game Command Tests")
class StartGameCommandTest {

    @Test
    @DisplayName("Should be successfully instantiated")
    void shouldInstantiate() {
        // Act
        StartGameCommand command = new StartGameCommand();

        // Assert
        assertNotNull(command);
    }

    @Test
    @DisplayName("Equality: All instances of an empty command should be equal")
    void shouldEvaluateEqualityByValue() {
        // Arrange
        StartGameCommand cmd1 = new StartGameCommand();
        StartGameCommand cmd2 = new StartGameCommand();

        // Assert
        assertEquals(cmd1, cmd2);
        assertEquals(cmd1.hashCode(), cmd2.hashCode());
    }

    @Test
    @DisplayName("toString should reflect the command name in standard record format")
    void shouldHaveDescriptiveToString() {
        // Arrange
        StartGameCommand command = new StartGameCommand();

        // Assert
        assertEquals("StartGameCommand[]", command.toString());
    }
}