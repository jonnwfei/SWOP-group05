package base.domain.commands;

import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import base.domain.commands.GameCommand;
import base.domain.commands.GameCommand.*;
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
            assertEquals(ACE_OF_SPADES, command.card());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException if card is null")
        void shouldRejectNullCard() {
            // Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new CardCommand(null));
            assertTrue(exception.getMessage().contains("card cannot be null"));
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

            // Assert
            assertEquals(cmd1, cmd2);
            assertEquals(cmd1.hashCode(), cmd2.hashCode());
            assertNotEquals(cmd1, cmd3);
        }

        @Test
        @DisplayName("toString should clearly show the encapsulated card")
        void shouldHaveDescriptiveToString() {
            CardCommand command = new CardCommand(ACE_OF_SPADES);
            String commandString = command.toString();

            // Assert
            assertTrue(commandString.contains("CardCommand"));
            assertTrue(commandString.contains("ACE"));
            assertTrue(commandString.contains("SPADES"));
        }
    }
}