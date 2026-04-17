package base.domain.commands;

import base.domain.bid.BidType;
import base.domain.card.Suit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
            assertThat(command.bid()).isEqualTo(BidType.SOLO);
            assertThat(command.suit()).isEqualTo(Suit.HEARTS);
        }

        @ParameterizedTest(name = "Single-argument constructor for {0} should set suit to null")
        @EnumSource(BidType.class)
        void shouldInitializeWithNullSuitUsingShortConstructor(BidType type) {
            // Act
            BidCommand command = new BidCommand(type);

            // Assert
            assertThat(command.bid()).isEqualTo(type);
            assertThat(command.suit()).isNull();
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException if BidType is null")
        void shouldRejectNullBid() {
            assertThatThrownBy(() -> new BidCommand(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("bid cannot be null");
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

            assertThat(cmd1)
                    .isEqualTo(cmd2)
                    .hasSameHashCodeAs(cmd2)
                    .isNotEqualTo(cmd3);
        }
    }
}