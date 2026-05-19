package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.PlayerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Miserie Bid Rules & Calculations")
class MiserieBidTest {

    private BidType miserieBidType;
    private MiserieBid bid;

    @BeforeEach
    void setUp() {
        // Arrange
        miserieBidType = BidType.MISERIE;
        bid = new MiserieBid(miserieBidType);
    }

    @Test
    @DisplayName("Constructor enforces non-null BidType")
    void constructor_NullParameters_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                        new MiserieBid(null),
                "Should reject null BidType"
        );
    }

    @Test
    @DisplayName("Constructor rejects BidTypes outside the MISERIE category")
    void constructor_InvalidBidCategory_ThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new MiserieBid( BidType.ABONDANCE_9)
        );
        assertTrue(exception.getMessage().contains("MISERIE category"));
    }

    @Test
    @DisplayName("Basic Accessors return correctly assigned values")
    void basicAccessors_ReturnExpectedValues() {
        assertEquals(miserieBidType, bid.getType(), "getType() should return the assigned BidType");
        assertEquals(miserieBidType, bid.bidType(), "Record accessor should return the assigned BidType");
    }

    @Test
    @DisplayName("determineTrump() always returns null (No Trumps)")
    void determineTrump_AlwaysReturnsNull() {
        assertNull(bid.determineTrump(Suit.SPADES), "Miserie ignores dealt trump and returns null");
        assertNull(bid.determineTrump(null), "Miserie returns null even if dealt trump is null");
    }

    @Test
    @DisplayName("Winning the contract (0 tricks) yields positive base points")
    void calculateBasePoints_ZeroTricks_ReturnsPositivePoints() {
        int expectedPoints = miserieBidType.getBasePoints();
        assertEquals(expectedPoints, bid.calculateBasePoints(0));
    }

    @ParameterizedTest(name = "Failing contract by taking {0} tricks yields negative base points")
    @ValueSource(ints = {1, 2, 7, 13})
    void calculateBasePoints_OneOrMoreTricks_ReturnsNegativePoints(int tricksWon) {
        int expectedPenalty = -1 * miserieBidType.getBasePoints();
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