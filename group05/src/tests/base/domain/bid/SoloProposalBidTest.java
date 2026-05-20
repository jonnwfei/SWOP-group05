package base.domain.bid;

import base.domain.card.Suit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Solo Proposal Bid Rules & Calculations")
class SoloProposalBidTest {

    private Suit dealtTrump;
    private SoloProposalBid bid;

    @BeforeEach
    void setUp() {
        // Arrange
        dealtTrump = Suit.DIAMONDS;
        bid = new SoloProposalBid();
    }

    @Test
    @DisplayName("Constructor creates instance successfully (no parameters to validate)")
    void constructor_NullParameters_ThrowsIllegalArgumentException() {
        assertNotNull(new SoloProposalBid(), "SoloProposalBid is a no-arg record and should always construct successfully");
    }

    @Test
    @DisplayName("Basic Accessors return correctly assigned values")
    void basicAccessors_ReturnExpectedValues() {
        assertEquals(BidType.SOLO_PROPOSAL, bid.getType(), "getType() should always return SOLO_PROPOSAL");
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