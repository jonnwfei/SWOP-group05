package base.domain.player;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PlayerId Value Object Validation")
class PlayerIdTest {

    @Nested
    @DisplayName("Constructor Constraints")
    class ConstructorTests {

        @Test
        @DisplayName("Should successfully create PlayerId with a valid UUID")
        void shouldCreateWithValidUUID() {
            UUID expectedId = UUID.randomUUID();
            PlayerId playerId = new PlayerId(expectedId);

            assertNotNull(playerId);
            assertEquals(expectedId, playerId.id());
        }

        @Test
        @DisplayName("Should successfully auto-generate a UUID when using the empty constructor")
        void shouldAutoGenerateUUID() {
            PlayerId playerId = new PlayerId();

            assertNotNull(playerId);
            assertNotNull(playerId.id());
        }

        @Test
        @DisplayName("Should throw exception when UUID is null in the primary constructor")
        void shouldThrowWhenUUIDIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new PlayerId(null)
            );

            assertEquals("PlayerId cannot be null", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Factory Method (fromString) Constraints")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should successfully create PlayerId from a valid UUID string")
        void shouldCreateFromValidString() {
            String uuidString = UUID.randomUUID().toString();
            PlayerId playerId = PlayerId.fromString(uuidString);

            assertNotNull(playerId);
            assertEquals(uuidString, playerId.id().toString());
        }

        @Test
        @DisplayName("Should throw exception when parsing a null string")
        void shouldThrowWhenStringIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> PlayerId.fromString(null)
            );

            assertEquals("String ID cannot be null or empty", exception.getMessage());
        }

        @ParameterizedTest(name = "Throws exception for blank input: ''{0}''")
        @ValueSource(strings = {"", "   ", "\t", "\n"})
        @DisplayName("Should throw exception when parsing an empty or whitespace-only string")
        void shouldThrowWhenStringIsBlank(String blankId) {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> PlayerId.fromString(blankId)
            );

            assertEquals("String ID cannot be null or empty", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when parsing a string that is not a valid UUID format")
        void shouldThrowWhenStringIsInvalidUUIDFormat() {
            String invalidUUID = "player-123";

            // UUID.fromString naturally throws an IllegalArgumentException for bad formats
            assertThrows(
                    IllegalArgumentException.class,
                    () -> PlayerId.fromString(invalidUUID)
            );
        }
    }
}