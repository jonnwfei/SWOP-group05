package base.domain.results;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import base.domain.results.BidResults.*;
import base.domain.results.CountResults.*;
import base.domain.results.PlayResults.*;
@DisplayName("Amount Of Trick Won Result Tests")
class AmountOfTrickWonResultTest {

    @Test
    @DisplayName("Should be successfully instantiated")
    void shouldInstantiate() {
        // Act
        AmountOfTrickWonResult result = new AmountOfTrickWonResult();

        // Assert
        assertNotNull(result);
    }

    @Test
    @DisplayName("Equality: All instances of an empty result should be equal")
    void shouldEvaluateEqualityByValue() {
        // Arrange
        AmountOfTrickWonResult res1 = new AmountOfTrickWonResult();
        AmountOfTrickWonResult res2 = new AmountOfTrickWonResult();

        // Assert
        assertEquals(res1, res2);
        assertEquals(res1.hashCode(), res2.hashCode());
    }

    @Test
    @DisplayName("toString should reflect the class name in standard record format")
    void shouldHaveDescriptiveToString() {
        // Arrange
        AmountOfTrickWonResult result = new AmountOfTrickWonResult();

        // Assert
        assertEquals("AmountOfTrickWonResult[]", result.toString());
    }
}