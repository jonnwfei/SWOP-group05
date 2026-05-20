package base.domain.scores;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ScoringParameters Value Object Tests")
class ScoringParametersTest {

    @Nested
    @DisplayName("Constructor Validation & Defensive Programming")
    class ValidationTests {

        @Test
        @DisplayName("Successfully creates instance with valid parameters")
        void validParameters_Success() {
            assertDoesNotThrow(() -> new ScoringParameters(8, 13, 6, 3, true));
            assertDoesNotThrow(() -> new ScoringParameters(0, 0, 21, 0, false), "Miserie boundaries (0,0) should be valid");
        }

        @Test
        @DisplayName("Rejects negative minimum tricks")
        void negativeMinTricks_ThrowsException() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new ScoringParameters(-1, 13, 6, 3, true));
            assertTrue(ex.getMessage().contains("Minimum tricks cannot be negative"));
        }

        @Test
        @DisplayName("Rejects max tricks being lower than min tricks")
        void invertedTrickBounds_ThrowsException() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new ScoringParameters(10, 8, 6, 3, true));
            assertTrue(ex.getMessage().contains("Maximum tricks cannot be less than minimum"));
        }

        @Test
        @DisplayName("Rejects negative base points")
        void negativeBasePoints_ThrowsException() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new ScoringParameters(8, 13, -5, 3, true));
            assertTrue(ex.getMessage().contains("Base points must be positive"));
        }

        @Test
        @DisplayName("Rejects negative overtrick points")
        void negativeOvertrickPoints_ThrowsException() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new ScoringParameters(8, 13, 6, -1, true));
            assertTrue(ex.getMessage().contains("Overtrick points cannot be negative"));
        }
    }

    @Nested
    @DisplayName("Point Calculation Mechanics")
    class CalculationTests {

        @Test
        @DisplayName("Loss: Returns exactly negative base points when falling short of minTricks")
        void calculatePoints_Loss_UnderMin() {
            // Proposal (alone): Needs 6, gets 5
            ScoringParameters params = new ScoringParameters(6, 13, 6, 3, true);
            assertEquals(-6, params.calculatePoints(5));
            assertEquals(-6, params.calculatePoints(0));
        }

        @Test
        @DisplayName("Loss: Returns exactly negative base points when exceeding maxTricks (Miserie case)")
        void calculatePoints_Loss_OverMax() {
            // Miserie: Needs 0, max 0. Gets 1.
            ScoringParameters params = new ScoringParameters(0, 0, 21, 0, false);
            assertEquals(-21, params.calculatePoints(1));
            assertEquals(-21, params.calculatePoints(13));
        }

        @ParameterizedTest(name = "Won {0} tricks -> {1} points (Base:6, Overtrick:3)")
        @CsvSource({
                "6, 6",   // Exactly min tricks -> Base points
                "7, 9",   // 1 overtrick -> 6 + (1 * 3) = 9
                "8, 12",  // 2 overtricks -> 6 + (2 * 3) = 12
                "10, 18"  // 4 overtricks -> 6 + (4 * 3) = 18
        })
        @DisplayName("Standard Win: Accurately scales points for exact matches and overtricks")
        void calculatePoints_StandardWin(int tricksWon, int expectedPoints) {
            ScoringParameters params = new ScoringParameters(6, 13, 6, 3, false);
            assertEquals(expectedPoints, params.calculatePoints(tricksWon));
        }

        @Test
        @DisplayName("All Tricks Doubled: Applies 2x multiplier if flag is true and 13 tricks won")
        void calculatePoints_AllTricks_Doubled() {
            // Proposal (alone): Needs 6, Base 6, Overtrick 3. Max 13 tricks.
            // Calculation: 6 + (7 * 3) = 27. Doubled = 54.
            ScoringParameters params = new ScoringParameters(6, 13, 6, 3, true);

            assertEquals(54, params.calculatePoints(13));
        }

        @Test
        @DisplayName("All Tricks Doubled (False): Does not apply 2x multiplier if flag is false")
        void calculatePoints_AllTricks_NotDoubled() {
            // Abondance 9: Needs 9, Base 15, Overtrick 0. Max 13 tricks.
            // Calculation: 15 + (4 * 0) = 15. Not doubled = 15.
            ScoringParameters params = new ScoringParameters(9, 13, 15, 0, false);

            assertEquals(15, params.calculatePoints(13));
        }
    }
}