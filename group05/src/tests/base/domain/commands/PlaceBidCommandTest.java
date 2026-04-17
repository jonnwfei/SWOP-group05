package base.domain.commands;

import base.domain.bid.BidType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Place Bid Command Tests")
class PlaceBidCommandTest {

    @Nested
    @DisplayName("Constructor & Validation")
    class ConstructorTests {

        @ParameterizedTest(name = "Should successfully create command for BidType: {0}")
        @EnumSource(BidType.class)
        void shouldAcceptAllValidBidTypes(BidType type) {
            // Act
            PlaceBidCommand command = new PlaceBidCommand(type);

            // Assert
            assertEquals(type, command.bidType());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException if BidType is null")
        void shouldRejectNullBidType() {
            // Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new PlaceBidCommand(null));
            assertTrue(exception.getMessage().contains("bidType cannot be null"));
        }
    }

    @Nested
    @DisplayName("Record Behavior")
    class RecordBehaviorTests {

        @Test
        @DisplayName("Equality should be value-based")
        void shouldEvaluateEqualityByValue() {
            PlaceBidCommand cmd1 = new PlaceBidCommand(BidType.SOLO);
            PlaceBidCommand cmd2 = new PlaceBidCommand(BidType.SOLO);
            PlaceBidCommand cmd3 = new PlaceBidCommand(BidType.PASS);

            // Assert
            assertEquals(cmd1, cmd2);
            assertEquals(cmd1.hashCode(), cmd2.hashCode());
            assertNotEquals(cmd1, cmd3);
        }
    }
}