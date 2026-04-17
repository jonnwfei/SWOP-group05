package base.domain.commands;

import base.domain.player.PlayerId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
            assertThat(command.playerIds())
                    .hasSize(2)
                    .containsExactly(P1, P2);
        }

        @Test
        @DisplayName("Should allow an empty list (representing 'no selection')")
        void shouldAcceptEmptyList() {
            // Act
            PlayerListCommand command = new PlayerListCommand(List.of());

            // Assert
            assertThat(command.playerIds()).isEmpty();
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException if playerIds list is null")
        void shouldRejectNullList() {
            // Assert
            assertThatThrownBy(() -> new PlayerListCommand(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("players cannot be null");
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

            assertThat(cmd1)
                    .isEqualTo(cmd2)
                    .hasSameHashCodeAs(cmd2)
                    .isNotEqualTo(cmd3);
        }

        @Test
        @DisplayName("toString should contain the list of IDs")
        void shouldHaveDescriptiveToString() {
            PlayerListCommand command = new PlayerListCommand(List.of(P1));

            assertThat(command.toString())
                    .contains("PlayerListCommand")
                    .contains("p1");
        }
    }
}