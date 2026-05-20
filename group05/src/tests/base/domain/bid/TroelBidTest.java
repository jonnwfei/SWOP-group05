package base.domain.bid;

import base.domain.card.Card;
import base.domain.card.Rank;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @ParameterizedTest(name = "TROEL (Target 8) - Winning {0} tricks yields {1} points (Base 4 + 2/overtrick)")
    @CsvSource({
            "8, 4",    // Exact target
            "10, 8",   // 2 Overtricks -> 4 + (2 * 2) = 8
    })
    void calculateBasePoints_TroelSuccess_ReturnsPoints(int tricksWon, int expectedPoints) {
        TroelBid bid = new TroelBid( BidType.TROEL, Suit.CLUBS);
        assertEquals(expectedPoints, bid.calculateBasePoints(tricksWon));
    }

    @ParameterizedTest(name = "TROELA (Target 9) - Winning {0} tricks yields {1} points (Base 4 + 2/overtrick)")
    @CsvSource({
            "9, 4",    // Exact target
            "11, 8",   // 2 Overtricks -> 4 + (2 * 2) = 8
    })
    void calculateBasePoints_TroelaSuccess_ReturnsPoints(int tricksWon, int expectedPoints) {
        TroelBid bid = new TroelBid( BidType.TROELA, Suit.HEARTS);
        assertEquals(expectedPoints, bid.calculateBasePoints(tricksWon));
    }

    @Test
    @DisplayName("Winning all 13 tricks (Slam) doubles the total accumulated points")
    void calculateBasePoints_SlamAchieved_DoublesTotalPoints() {
        TroelBid bid = new TroelBid( BidType.TROEL, Suit.CLUBS);

        // Base(4) + 5 Overtricks(10) = 14 points. Doubled for Slam = 28 points.
        assertEquals(28, bid.calculateBasePoints(13));
    }

    @ParameterizedTest(name = "Failing contract yields negative base points (-4)")
    @ValueSource(ints = {7, 4, 0})
    void calculateBasePoints_BelowTarget_ReturnsNegativeBasePoints(int tricksWon) {
        TroelBid bid = new TroelBid( BidType.TROEL, Suit.CLUBS);
        int expectedPenalty = -1 * BidType.TROEL.getBasePoints();
        assertEquals(expectedPenalty, bid.calculateBasePoints(tricksWon));
    }

    @Test
    @DisplayName("calculateBasePoints() rejects negative trick counts")
    void calculateBasePoints_NegativeInput_ThrowsIllegalArgumentException() {
        TroelBid bid = new TroelBid( BidType.TROEL, Suit.CLUBS);
        assertThrows(IllegalArgumentException.class, () -> bid.calculateBasePoints(-1));
    }
}