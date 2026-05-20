package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.PlayerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Troel & Troela Bid Rules & Calculations")
class TroelBidTest {

    private PlayerId partnerId;

    @BeforeEach
    void setUp() {
        partnerId = new PlayerId();
    }

    @Test
    @DisplayName("Constructor enforces non-null BidType and correct category")
    void constructor_InvalidParameters_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                        new TroelBid(null, Suit.SPADES),
                "Should reject null BidType"
        );

        assertThrows(IllegalArgumentException.class, () ->
                        new TroelBid(BidType.SOLO, Suit.SPADES),
                "Should reject BidTypes outside the TROEL category"
        );
    }

    @Test
    @DisplayName("TROEL requires an explicit missing Ace suit")
    void constructor_TroelNullSuit_ThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new TroelBid(BidType.TROEL, null)
        );
        assertTrue(exception.getMessage().contains("requires the suit of the missing Ace"));
    }

    @Test
    @DisplayName("TROELA automatically overrides trump suit to Hearts")
    void constructor_Troela_AlwaysSetsTrumpToHearts() {
        // Even if we pass null or SPADES, TROELA always forces Hearts
        TroelBid bid = new TroelBid(BidType.TROELA, Suit.SPADES);
        assertEquals(Suit.HEARTS, bid.determineTrump(Suit.CLUBS), "TROELA trump must always be Hearts");
    }

    @Test
    @DisplayName("Basic Accessors return correctly assigned values")
    void basicAccessors_ReturnExpectedValues() {
        TroelBid bid = new TroelBid( BidType.TROEL, Suit.CLUBS);

        assertEquals(BidType.TROEL, bid.getType());
        assertEquals(BidType.TROEL, bid.bidType());
        assertEquals(Suit.CLUBS, bid.trumpSuit());
        assertEquals(Suit.CLUBS, bid.determineTrump(Suit.HEARTS));
    }
}