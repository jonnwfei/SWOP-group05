package base.domain.commands;

import base.domain.card.Suit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
            assertThat(command.suit()).isEqualTo(suit);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException if Suit is null")
        void shouldRejectNullSuit() {
            // Assert
            assertThatThrownBy(() -> new SuitCommand(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("suit cannot be null");
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

            assertThat(cmd1)
                    .isEqualTo(cmd2)
                    .hasSameHashCodeAs(cmd2)
                    .isNotEqualTo(cmd3);
        }

        @Test
        @DisplayName("toString should contain the suit name")
        void shouldHaveDescriptiveToString() {
            SuitCommand command = new SuitCommand(Suit.DIAMONDS);

            assertThat(command.toString())
                    .contains("SuitCommand")
                    .contains("DIAMONDS");
        }
    }
}