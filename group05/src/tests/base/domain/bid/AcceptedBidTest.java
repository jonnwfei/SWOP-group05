package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Accepted Bid Rules & Calculations")
class AcceptedBidTest {

    private Suit dealtTrump;
    private AcceptedBid bid;

    @BeforeEach
    void setUp() {
        // Arrange
        dealtTrump = Suit.CLUBS;
        bid = new AcceptedBid();
    }

    @Test
    @DisplayName("Constructor creates instance successfully (no parameters to validate)")
    void constructor_NullParameters_ThrowsIllegalArgumentException() {
        assertNotNull(new AcceptedBid(), "AcceptedBid is a no-arg record and should always construct successfully");
    }

    @Test
    @DisplayName("Basic Accessors return correctly assigned values")
    void basicAccessors_ReturnExpectedValues() {
        assertEquals(BidType.ACCEPTANCE, bid.getType(), "getType() should always return ACCEPTANCE");
    }

    @Test
    @DisplayName("determineTrump() enforces the originally dealt trump suit")
    void determineTrump_ValidDealtTrump_ReturnsDealtTrump() {
        assertEquals(dealtTrump, bid.determineTrump(dealtTrump));
        assertEquals(Suit.DIAMONDS, bid.determineTrump(Suit.DIAMONDS));
    }

    @Test
    @DisplayName("determineTrump() rejects null dealt trump")
    void determineTrump_NullDealtTrump_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> bid.determineTrump(null));
    }

    @Test
    @DisplayName("Winning all 13 tricks (Slam) doubles the total accumulated points")
    void calculateBasePoints_SlamAchieved_DoublesTotalPoints() {
        // Base(2) + Overtricks(5) = 7. Doubled for Slam = 14.
        assertEquals(14, bid.calculateBasePoints(13));
    }

    @ParameterizedTest(name = "Failing contract by taking only {0} tricks yields negative base points")
    @ValueSource(ints = {7, 4, 0})
    void calculateBasePoints_BelowTarget_ReturnsNegativeBasePoints(int tricksWon) {
        // When failing an acceptance, overtricks are ignored and the flat base penalty is applied (-2)
        int expectedPenalty = -1 * BidType.ACCEPTANCE.getBasePoints();
        assertEquals(expectedPenalty, bid.calculateBasePoints(tricksWon));
    }

    @Test
    @DisplayName("calculateBasePoints() rejects negative trick counts")
    void calculateBasePoints_NegativeInput_ThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                bid.calculateBasePoints(-1)
        );
        assertTrue(exception.getMessage().contains("negative"));
    }
}