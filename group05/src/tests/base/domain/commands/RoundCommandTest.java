package base.domain.commands;

import base.domain.round.Round;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("Round Command Validation & State")
class RoundCommandTest {

    @Nested
    @DisplayName("Constructor Constraints")
    class ConstructorTests {

        @Test
        @DisplayName("Should successfully create a RoundCommand with a valid Round")
        void shouldCreateWithValidRound() {
            // Arrange
            Round mockRound = mock(Round.class);

            // Act
            RoundCommand command = new RoundCommand(mockRound);

            // Assert
            assertNotNull(command, "Command should be successfully instantiated.");

            // Note: If RoundCommand is a standard class instead of a record,
            // change `.round()` to `.getRound()`
            assertEquals(mockRound, command.round(), "The command should retain the exact Round instance provided.");
        }

        @Test
        @DisplayName("Defensive constructor should reject a null Round")
        void shouldRejectNullRound() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> new RoundCommand(null)
            );

            // Asserting the message ensures the domain is failing for the right reason
            assertTrue(exception.getMessage().toLowerCase().contains("null"),
                    "Exception message should mention that the input was null.");
        }
    }

    @Nested
    @DisplayName("Equality and Identity")
    class EqualityTests {

        @Test
        @DisplayName("Commands wrapping the same Round instance should be equal")
        void shouldBeEqualForSameRound() {
            // Arrange
            Round mockRound = mock(Round.class);
            RoundCommand command1 = new RoundCommand(mockRound);
            RoundCommand command2 = new RoundCommand(mockRound);

            // Assert
            assertEquals(command1, command2, "Two commands wrapping the exact same Round reference should evaluate as equal.");
            assertEquals(command1.hashCode(), command2.hashCode(), "Equal commands must share the same hash code.");
        }

        @Test
        @DisplayName("Commands wrapping different Round instances should not be equal")
        void shouldNotBeEqualForDifferentRounds() {
            // Arrange
            Round mockRound1 = mock(Round.class);
            RoundCommand command1 = new RoundCommand(mockRound1);

            Round mockRound2 = mock(Round.class);
            RoundCommand command2 = new RoundCommand(mockRound2);

            // Assert
            assertNotEquals(command1, command2, "Commands wrapping different Round instances must not evaluate as equal.");
        }
    }
}