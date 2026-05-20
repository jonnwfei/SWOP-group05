package tests.base.domain.bid;

import base.domain.bid.BidType;
import base.domain.bid.ProposalBid;
import base.domain.card.Suit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Proposal Bid Rules & Calculations")
class ProposalBidTest {
    
    private Suit dealtTrump;
    private ProposalBid bid;

    @BeforeEach
    void setUp() {
        // Arrange
        dealtTrump = Suit.HEARTS;
        bid = new ProposalBid();
    }

    @Test
    @DisplayName("Constructor creates instance successfully (no parameters to validate)")
    void constructor_NullParameters_ThrowsIllegalArgumentException() {
        assertNotNull(new ProposalBid(), "ProposalBid is a no-arg record and should always construct successfully");
    }

    @Test
    @DisplayName("Basic Accessors return correctly assigned values")
    void basicAccessors_ReturnExpectedValues() {
        assertEquals(BidType.PROPOSAL, bid.getType(), "getType() should always return PROPOSAL");
    }

    @Test
    @DisplayName("determineTrump() enforces the originally dealt trump suit")
    void determineTrump_ValidDealtTrump_ReturnsDealtTrump() {
        assertEquals(dealtTrump, bid.determineTrump(dealtTrump));
        assertEquals(Suit.SPADES, bid.determineTrump(Suit.SPADES));
    }

    @Test
    @DisplayName("determineTrump() rejects null dealt trump")
    void determineTrump_NullDealtTrump_ThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                bid.determineTrump(null)
        );
        assertTrue(exception.getMessage().toLowerCase().contains("null"));
    }
}