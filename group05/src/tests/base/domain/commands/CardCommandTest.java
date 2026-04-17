package base.domain.commands;

import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Card Command Tests")
class CardCommandTest {

    private static final Card ACE_OF_SPADES = new Card(Suit.SPADES, Rank.ACE);

    @Nested
    @DisplayName("Constructor & Validation")
    class ConstructorTests {

        @Test
        @DisplayName("Should correctly assign the card in the constructor")
        void shouldAssignCard() {
            // Act
            CardCommand command = new CardCommand(ACE_OF_SPADES);

            // Assert
            assertThat(command.card()).isEqualTo(ACE_OF_SPADES);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException if card is null")
        void shouldRejectNullCard() {
            // Assert
            assertThatThrownBy(() -> new CardCommand(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("card cannot be null");
        }
    }

    @Nested
    @DisplayName("Record Behavior")
    class RecordBehaviorTests {

        @Test
        @DisplayName("Equality should be based on card value")
        void shouldEvaluateEqualityByValue() {
            CardCommand cmd1 = new CardCommand(ACE_OF_SPADES);
            CardCommand cmd2 = new CardCommand(new Card(Suit.SPADES, Rank.ACE));
            CardCommand cmd3 = new CardCommand(new Card(Suit.HEARTS, Rank.TWO));

            assertThat(cmd1)
                    .isEqualTo(cmd2)
                    .hasSameHashCodeAs(cmd2)
                    .isNotEqualTo(cmd3);
        }

        @Test
        @DisplayName("toString should clearly show the encapsulated card")
        void shouldHaveDescriptiveToString() {
            CardCommand command = new CardCommand(ACE_OF_SPADES);

            assertThat(command.toString())
                    .contains("CardCommand")
                    .contains("ACE")
                    .contains("SPADES");
        }
    }
}