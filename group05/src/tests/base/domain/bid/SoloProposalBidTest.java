package base.domain.bid;

import base.domain.card.Suit;
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

    @ParameterizedTest(name = "Winning {0} tricks yields {1} base points (Base 6 + 3 per overtrick)")
    @CsvSource({
            "5, 6",    // Target met exactly (6 points)
            "6, 9",    // 1 Overtrick (6 + 3 = 9)
            "12, 27"   // 7 Overtricks (6 + 21 = 27)
    })
    void calculateBasePoints_TargetMet_ReturnsBasePlusOvertricks(int tricksWon, int expectedPoints) {
        assertEquals(expectedPoints, bid.calculateBasePoints(tricksWon));
    }

    @Test
    @DisplayName("Winning all 13 tricks (Slam) doubles the total accumulated points")
    void calculateBasePoints_SlamAchieved_DoublesTotalPoints() {
        // Base(6) + 8 Overtricks(24) = 30 points. Doubled for Slam = 60 points.
        assertEquals(60, bid.calculateBasePoints(13));
    }

    @ParameterizedTest(name = "Failing contract by taking only {0} tricks yields negative base points")
    @ValueSource(ints = {4, 1, 0})
    void calculateBasePoints_BelowTarget_ReturnsNegativeBasePoints(int tricksWon) {
        // When failing a solo proposal, overtricks logic is skipped and the flat base penalty is applied (-6)
        int expectedPenalty = -1 * BidType.SOLO_PROPOSAL.getBasePoints();
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