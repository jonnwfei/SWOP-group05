package base.domain.bid;

import base.domain.card.Suit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Bid Interface Default Methods")
class BidTest {

    private Bid realBid(BidType type) {
        return type.instantiate(Suit.HEARTS);
    }

    @Test
    @DisplayName("compareTo() delegates to BidType: Lower vs Higher returns negative")
    void compareTo_LowerVsHigherBid_ReturnsNegative() {
        Bid lowerBid = realBid(BidType.PASS);
        Bid higherBid = realBid(BidType.SOLO);

        int result = lowerBid.compareTo(higherBid);

        assertTrue(result < 0, "A lower bid (PASS) compared to a higher bid (SOLO) should be < 0");
    }

    @Test
    @DisplayName("compareTo() delegates to BidType: Higher vs Lower returns positive")
    void compareTo_HigherVsLowerBid_ReturnsPositive() {
        Bid higherBid = realBid(BidType.SOLO);
        Bid lowerBid = realBid(BidType.PASS);

        int result = higherBid.compareTo(lowerBid);

        assertTrue(result > 0, "A higher bid (SOLO) compared to a lower bid (PASS) should be > 0");
    }

    @Test
    @DisplayName("compareTo() delegates to BidType: Equal bids return zero")
    void compareTo_EqualBids_ReturnsZero() {
        Bid bid1 = realBid(BidType.ABONDANCE_9);
        Bid bid2 = realBid(BidType.ABONDANCE_9);

        int result = bid1.compareTo(bid2);

        assertEquals(0, result, "Two bids of the exact same type should return 0");
    }
}