package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.PlayerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Solo Bid Rules & Calculations")
class SoloBidTest {

    private BidType soloBidType;
    private Suit chosenTrump;
    private SoloBid bid;

    @BeforeEach
    void setUp() {
        // Arrange
        soloBidType = BidType.SOLO;
        chosenTrump = Suit.DIAMONDS;

        bid = new SoloBid(soloBidType, chosenTrump);
    }

    @Test
    @DisplayName("Constructor enforces non-null BidType")
    void constructor_NullParameters_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                        new SoloBid(null, chosenTrump),
                "Should reject null BidType"
        );
    }

    @Test
    @DisplayName("Constructor rejects BidTypes outside the SOLO category")
    void constructor_InvalidCategory_ThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new SoloBid(BidType.MISERIE, chosenTrump)
        );
        assertTrue(exception.getMessage().contains("SOLO category"));
    }

    @Test
    @DisplayName("Basic Accessors return correctly assigned values")
    void basicAccessors_ReturnExpectedValues() {

        assertEquals(soloBidType, bid.getType(), "getType() should return the assigned BidType");
        assertEquals(soloBidType, bid.bidType(), "Record accessor should return the assigned BidType");

        assertEquals(chosenTrump, bid.trump(), "Record accessor should return the chosen trump suit");
    }

    @Test
    @DisplayName("determineTrump() overrides dealt trump with the player's chosen suit")
    void determineTrump_ValidDealtTrump_ReturnsChosenTrump() {
        // The dealt trump (e.g., SPADES) is ignored in favor of the chosen trump (DIAMONDS)
        assertEquals(chosenTrump, bid.determineTrump(Suit.SPADES));
    }

    @Test
    @DisplayName("determineTrump() rejects null dealt trump")
    void determineTrump_NullDealtTrump_ThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                bid.determineTrump(null)
        );
        assertTrue(exception.getMessage().toLowerCase().contains("null"));
    }

    @Test
    @DisplayName("Winning the contract (13 tricks) yields positive base points")
    void calculateBasePoints_TargetMet_ReturnsPositiveBasePoints() {
        int expectedPoints = soloBidType.getBasePoints();
        int target = soloBidType.getTargetTricks(); // For Solo, this is exactly 13

        // Scenario: Exact target achieved
        assertEquals(expectedPoints, bid.calculateBasePoints(target));
    }

    @ParameterizedTest(name = "Failing the contract by taking {0} tricks yields negative base points")
    @ValueSource(ints = {12, 6, 0})
    void calculateBasePoints_BelowTarget_ReturnsNegativeBasePoints(int tricksWon) {
        int expectedPenalty = -1 * soloBidType.getBasePoints();

        // Scenario: Missing the 13-trick requirement by any amount results in the penalty
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