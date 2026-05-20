package base.domain.bid;

import base.domain.card.Suit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
}