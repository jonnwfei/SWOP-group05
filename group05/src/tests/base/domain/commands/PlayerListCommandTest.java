package base.domain.commands;

import base.domain.player.PlayerId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Player List Command Tests")
class PlayerListCommandTest {

    private static final PlayerId P1 = new PlayerId("p1");
    private static final PlayerId P2 = new PlayerId("p2");

    @Nested
    @DisplayName("Constructor & Validation")
    class ConstructorTests {

        @Test
        @DisplayName("Should successfully create command with a valid list of IDs")
        void shouldAcceptValidPlayerList() {
            // Arrange
            List<PlayerId> ids = List.of(P1, P2);

            // Act
            PlayerListCommand command = new PlayerListCommand(ids);

            // Assert
            assertEquals(2, command.playerIds().size());
            assertEquals(P1, command.playerIds().get(0));
            assertEquals(P2, command.playerIds().get(1));
            // Alternatively, checking the whole list equality
            assertEquals(ids, command.playerIds());
        }

        @Test
        @DisplayName("Should allow an empty list (representing 'no selection')")
        void shouldAcceptEmptyList() {
            // Act
            PlayerListCommand command = new PlayerListCommand(List.of());

            // Assert
            assertTrue(command.playerIds().isEmpty());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException if playerIds list is null")
        void shouldRejectNullList() {
            // Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new PlayerListCommand(null));
            assertTrue(exception.getMessage().contains("players cannot be null"));
        }
    }

    @Nested
    @DisplayName("Record Behavior")
    class RecordBehaviorTests {

        @Test
        @DisplayName("Equality should be based on list contents and order")
        void shouldEvaluateEqualityByValue() {
            PlayerListCommand cmd1 = new PlayerListCommand(List.of(P1, P2));
            PlayerListCommand cmd2 = new PlayerListCommand(List.of(P1, P2));
            PlayerListCommand cmd3 = new PlayerListCommand(List.of(P2, P1)); // Different order

            // Assert
            assertEquals(cmd1, cmd2);
            assertEquals(cmd1.hashCode(), cmd2.hashCode());
            assertNotEquals(cmd1, cmd3);
        }

        @Test
        @DisplayName("toString should contain the list of IDs")
        void shouldHaveDescriptiveToString() {
            PlayerListCommand command = new PlayerListCommand(List.of(P1));
            String commandString = command.toString();

            // Assert
            assertTrue(commandString.contains("PlayerListCommand"));
            assertTrue(commandString.contains("p1"));
        }
    }
}