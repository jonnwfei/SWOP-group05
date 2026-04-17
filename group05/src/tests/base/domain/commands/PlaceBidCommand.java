package base.domain.commands;

import base.domain.bid.BidType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
            assertThat(command.bidType()).isEqualTo(type);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException if BidType is null")
        void shouldRejectNullBidType() {
            // Assert
            assertThatThrownBy(() -> new PlaceBidCommand(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("bidType cannot be null");
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

            assertThat(cmd1)
                    .isEqualTo(cmd2)
                    .hasSameHashCodeAs(cmd2)
                    .isNotEqualTo(cmd3);
        }
    }
}