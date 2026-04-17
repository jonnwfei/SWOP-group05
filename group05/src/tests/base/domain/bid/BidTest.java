package base.domain.bid;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Bid Interface Default Methods")
class BidTest {

    /**
     * Helper method to generate an isolated mock of the Bid interface.
     * It stubs getType() to return the requested type, and tells Mockito
     * to execute the actual real code for the default compareTo() method.
     */
    private Bid createMockBid(BidType type) {
        Bid mockBid = mock(Bid.class);
        when(mockBid.getType()).thenReturn(type);
        when(mockBid.compareTo(any())).thenCallRealMethod();
        return mockBid;
    }

    @Test
    @DisplayName("compareTo() delegates to BidType: Lower vs Higher returns negative")
    void compareTo_LowerVsHigherBid_ReturnsNegative() {
        // Arrange
        Bid lowerBid = createMockBid(BidType.PASS);
        Bid higherBid = createMockBid(BidType.SOLO);

        // Act
        int result = lowerBid.compareTo(higherBid);

        // Assert
        assertTrue(result < 0, "A lower bid (PASS) compared to a higher bid (SOLO) should be < 0");
    }

    @Test
    @DisplayName("compareTo() delegates to BidType: Higher vs Lower returns positive")
    void compareTo_HigherVsLowerBid_ReturnsPositive() {
        // Arrange
        Bid higherBid = createMockBid(BidType.SOLO);
        Bid lowerBid = createMockBid(BidType.PASS);

        // Act
        int result = higherBid.compareTo(lowerBid);

        // Assert
        assertTrue(result > 0, "A higher bid (SOLO) compared to a lower bid (PASS) should be > 0");
    }

    @Test
    @DisplayName("compareTo() delegates to BidType: Equal bids return zero")
    void compareTo_EqualBids_ReturnsZero() {
        // Arrange
        Bid bid1 = createMockBid(BidType.ABONDANCE_9);
        Bid bid2 = createMockBid(BidType.ABONDANCE_9);

        // Act
        int result = bid1.compareTo(bid2);

        // Assert
        assertEquals(0, result, "Two bids of the exact same type should return 0");
    }
}