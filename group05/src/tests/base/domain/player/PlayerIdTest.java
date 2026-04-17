package base.domain.player;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PlayerId Value Object Validation")
class PlayerIdTest {

    @Nested
    @DisplayName("Constructor Validation Constraints")
    class ConstructorTests {

        @Test
        @DisplayName("Should successfully create PlayerId with a valid string")
        void shouldCreateValidPlayerId() {
            String expectedId = "player-123";
            PlayerId playerId = new PlayerId(expectedId);

            assertNotNull(playerId);
            assertEquals(expectedId, playerId.id());
        }

        @Test
        @DisplayName("Should throw exception when id is null")
        void shouldThrowWhenIdIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new PlayerId(null)
            );

            assertEquals("PlayerId cannot be null or empty", exception.getMessage());
        }

        @ParameterizedTest(name = "Throws exception for blank input: ''{0}''")
        @ValueSource(strings = {"", "   ", "\t", "\n"})
        @DisplayName("Should throw exception when id is empty or only contains whitespace")
        void shouldThrowWhenIdIsBlank(String blankId) {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new PlayerId(blankId)
            );

            assertEquals("PlayerId cannot be null or empty", exception.getMessage());
        }
    }
}