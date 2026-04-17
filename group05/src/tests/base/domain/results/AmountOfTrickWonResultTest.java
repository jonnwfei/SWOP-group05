package base.domain.results;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Amount Of Trick Won Result Tests")
class AmountOfTrickWonResultTest {

    @Test
    @DisplayName("Should be successfully instantiated")
    void shouldInstantiate() {
        // Act
        AmountOfTrickWonResult result = new AmountOfTrickWonResult();

        // Assert
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Equality: All instances of an empty result should be equal")
    void shouldEvaluateEqualityByValue() {
        // Arrange
        AmountOfTrickWonResult res1 = new AmountOfTrickWonResult();
        AmountOfTrickWonResult res2 = new AmountOfTrickWonResult();

        // Assert
        assertThat(res1)
                .isEqualTo(res2)
                .hasSameHashCodeAs(res2);
    }

    @Test
    @DisplayName("toString should reflect the class name in standard record format")
    void shouldHaveDescriptiveToString() {
        // Arrange
        AmountOfTrickWonResult result = new AmountOfTrickWonResult();

        // Assert
        assertThat(result.toString()).isEqualTo("AmountOfTrickWonResult[]");
    }
}