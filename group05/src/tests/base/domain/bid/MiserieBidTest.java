package base.domain.bid;

import base.domain.card.Suit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
}