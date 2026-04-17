package base.domain.results;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Bidding Completed Result Tests")
class BiddingCompletedTest {

    @Test
    @DisplayName("Should be successfully instantiated")
    void shouldInstantiate() {
        // Act
        BiddingCompleted result = new BiddingCompleted();

        // Assert
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Equality: Empty results of the same type should be equal")
    void shouldEvaluateEqualityByValue() {
        // Arrange
        BiddingCompleted res1 = new BiddingCompleted();
        BiddingCompleted res2 = new BiddingCompleted();

        // Assert
        assertThat(res1)
                .isEqualTo(res2)
                .hasSameHashCodeAs(res2);
    }

    @Test
    @DisplayName("toString should match the record class name")
    void shouldHaveDescriptiveToString() {
        // Arrange
        BiddingCompleted result = new BiddingCompleted();

        // Assert
        assertThat(result.toString()).isEqualTo("BiddingCompleted[]");
    }
}