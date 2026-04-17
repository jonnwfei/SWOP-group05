package base.domain.commands;

import base.domain.card.Suit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Suit Command Tests")
class SuitCommandTest {

    @Nested
    @DisplayName("Constructor & Validation")
    class ConstructorTests {

        @ParameterizedTest(name = "Should successfully create command for Suit: {0}")
        @EnumSource(Suit.class)
        void shouldAcceptAllValidSuits(Suit suit) {
            // Act
            SuitCommand command = new SuitCommand(suit);

            // Assert
            assertEquals(suit, command.suit());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException if Suit is null")
        void shouldRejectNullSuit() {
            // Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new SuitCommand(null));
            assertTrue(exception.getMessage().contains("suit cannot be null"));
        }
    }

    @Nested
    @DisplayName("Record Behavior")
    class RecordBehaviorTests {

        @Test
        @DisplayName("Equality should be value-based")
        void shouldEvaluateEqualityByValue() {
            SuitCommand cmd1 = new SuitCommand(Suit.HEARTS);
            SuitCommand cmd2 = new SuitCommand(Suit.HEARTS);
            SuitCommand cmd3 = new SuitCommand(Suit.SPADES);

            // Assert
            assertEquals(cmd1, cmd2);
            assertEquals(cmd1.hashCode(), cmd2.hashCode());
            assertNotEquals(cmd1, cmd3);
        }

        @Test
        @DisplayName("toString should contain the suit name")
        void shouldHaveDescriptiveToString() {
            SuitCommand command = new SuitCommand(Suit.DIAMONDS);
            String commandString = command.toString();

            // Assert
            assertTrue(commandString.contains("SuitCommand"));
            assertTrue(commandString.contains("DIAMONDS"));
        }
    }
}