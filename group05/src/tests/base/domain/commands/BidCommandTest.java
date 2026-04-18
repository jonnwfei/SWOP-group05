package base.domain.commands;

import base.domain.bid.BidType;
import base.domain.card.Suit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import base.domain.commands.GameCommand;
import base.domain.commands.GameCommand.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Bid Command Tests")
class BidCommandTest {

    @Nested
    @DisplayName("Constructor & Validation")
    class ConstructorTests {

        @Test
        @DisplayName("Full constructor should correctly assign bid and suit")
        void shouldAssignAllFields() {
            // Arrange & Act
            BidCommand command = new BidCommand(BidType.SOLO, Suit.HEARTS);

            // Assert
            assertEquals(BidType.SOLO, command.bid());
            assertEquals(Suit.HEARTS, command.suit());
        }

        @ParameterizedTest(name = "Single-argument constructor for {0} should set suit to null")
        @EnumSource(BidType.class)
        void shouldInitializeWithNullSuitUsingShortConstructor(BidType type) {
            // Act
            BidCommand command = new BidCommand(type);

            // Assert
            assertEquals(type, command.bid());
            assertNull(command.suit());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException if BidType is null")
        void shouldRejectNullBid() {
            // Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new BidCommand(null));
            assertTrue(exception.getMessage().contains("bid cannot be null"));
        }
    }

    @Nested
    @DisplayName("Record Behavior")
    class RecordBehaviorTests {

        @Test
        @DisplayName("Equality should be based on values, not reference")
        void shouldEvaluateEqualityByValue() {
            BidCommand cmd1 = new BidCommand(BidType.SOLO, Suit.CLUBS);
            BidCommand cmd2 = new BidCommand(BidType.SOLO, Suit.CLUBS);
            BidCommand cmd3 = new BidCommand(BidType.PROPOSAL, Suit.CLUBS);

            // Assert
            assertEquals(cmd1, cmd2);
            assertEquals(cmd1.hashCode(), cmd2.hashCode());
            assertNotEquals(cmd1, cmd3);
        }
    }
}