package base.domain.bid;

import base.domain.card.Suit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}