package tests.base.domain.bid;

import base.domain.bid.BidType;
import base.domain.bid.PassBid;
import base.domain.card.Suit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pass Bid Rules & Calculations")
class PassBidTest {

    private PassBid bid;

    @BeforeEach
    void setUp() {
        // Arrange
        bid = new PassBid();
    }

    @Test
    @DisplayName("Constructor creates instance successfully (no parameters to validate)")
    void constructor_NullPlayerId_ThrowsIllegalArgumentException() {
        assertNotNull(new PassBid(), "PassBid is a no-arg record and should always construct successfully");
    }

    @Test
    @DisplayName("Basic Accessors return correctly assigned values")
    void basicAccessors_ReturnExpectedValues() {
        assertEquals(BidType.PASS, bid.getType(), "getType() should always return PASS");
    }

    @Test
    @DisplayName("determineTrump() returns the originally dealt trump suit")
    void determineTrump_ValidDealtTrump_ReturnsDealtTrump() {
        assertEquals(Suit.HEARTS, bid.determineTrump(Suit.HEARTS));
        assertEquals(Suit.SPADES, bid.determineTrump(Suit.SPADES));
    }

    @Test
    @DisplayName("determineTrump() rejects null dealt trump")
    void determineTrump_NullDealtTrump_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> bid.determineTrump(null));
    }

    @Test
    @DisplayName("Records handle equality by value rather than memory reference")
    void testRecordEquality() {
        PassBid bid1 = new PassBid();
        PassBid bid2 = new PassBid();

        assertEquals(bid1, bid2, "Two PassBids with the same PlayerId should be considered equal");
    }
}