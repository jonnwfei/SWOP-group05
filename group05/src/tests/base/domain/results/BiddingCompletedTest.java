package base.domain.results;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Bidding Completed Result Tests")
class BiddingCompletedTest {

    @Test
    @DisplayName("Should be successfully instantiated")
    void shouldInstantiate() {
        // Act: Create the marker result [cite: 377]
        BiddingCompleted result = new BiddingCompleted();

        // Assert: Ensure it exists for the state transition [cite: 48]
        assertNotNull(result, "The result instance should not be null.");
    }

    @Test
    @DisplayName("Equality: Empty results of the same type should be equal")
    void shouldEvaluateEqualityByValue() {
        // Arrange
        BiddingCompleted res1 = new BiddingCompleted();
        BiddingCompleted res2 = new BiddingCompleted();

        // Assert: Records must handle equality by value [cite: 42, 44]
        assertEquals(res1, res2, "Instances of the same empty record should be equal.");
        assertEquals(res1.hashCode(), res2.hashCode(), "Hash codes for equal instances should match.");
    }

    @Test
    @DisplayName("toString should match the record class name")
    void shouldHaveDescriptiveToString() {
        // Arrange
        BiddingCompleted result = new BiddingCompleted();

        // Assert
        assertEquals("BiddingCompleted[]", result.toString(), "toString should follow standard record formatting.");
    }
}