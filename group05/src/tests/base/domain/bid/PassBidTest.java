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

@DisplayName("Pass Bid Rules & Calculations")
class PassBidTest {

    private PlayerId testPlayerId;
    private PassBid bid;

    @BeforeEach
    void setUp() {
        // Arrange
        testPlayerId = new PlayerId();
        bid = new PassBid(testPlayerId);
    }

    @Test
    @DisplayName("Constructor enforces non-null parameters")
    void constructor_NullPlayerId_ThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new PassBid(null)
        );
        assertTrue(exception.getMessage().toLowerCase().contains("player"),
                "Exception message should mention the null player.");
    }

    @Test
    @DisplayName("Basic Accessors return correctly assigned values")
    void basicAccessors_ReturnExpectedValues() {
        assertEquals(testPlayerId, bid.getPlayerId(), "getPlayerId() should return the passing player's ID");
        assertEquals(testPlayerId, bid.playerId(), "Record accessor should return the passing player's ID");
        assertEquals(BidType.PASS, bid.getType(), "getType() should always return PASS");
    }

    @Test
    @DisplayName("getTeam() returns only the passing player")
    void getTeam_AlwaysReturnsOnlyThePasser() {
        // Act
        // Pass bids ignore the history and player lists, so empty collections are safe to pass.
        List<PlayerId> team = bid.getTeam(Collections.emptyList(), Collections.emptyList());

        // Assert
        assertEquals(1, team.size(), "Team should consist of exactly 1 player");
        assertTrue(team.contains(testPlayerId), "Team must contain the passing player");
    }

    @Test
    @DisplayName("determineTrump() returns the originally dealt trump suit")
    void determineTrump_ValidDealtTrump_ReturnsDealtTrump() {
        assertEquals(Suit.HEARTS, bid.determineTrump(Suit.HEARTS));
        assertEquals(Suit.SPADES, bid.determineTrump(Suit.SPADES));
    }

    @Test
    @DisplayName("determineTrump() rejects null dealt trump")
    void determineTrump_NullDealtTrump_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> bid.determineTrump(null));
    }

    @ParameterizedTest(name = "Passing always yields {0} base points regardless of tricks won")
    @ValueSource(ints = {0, 1, 6, 13})
    void calculateBasePoints_AlwaysReturnsZero(int tricksWon) {
        // A PassBid always awards its base points (0) regardless of tricks accidentally won
        int expectedPoints = BidType.PASS.getBasePoints();
        assertEquals(expectedPoints, bid.calculateBasePoints(tricksWon));
    }

    @Test
    @DisplayName("calculateBasePoints() rejects negative trick counts")
    void calculateBasePoints_NegativeInput_ThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                bid.calculateBasePoints(-1)
        );
        assertTrue(exception.getMessage().contains("negative"), "Exception should mention negative tricks");
    }

    @Test
    @DisplayName("Records handle equality by value rather than memory reference")
    void testRecordEquality() {
        PassBid bid1 = new PassBid(testPlayerId);
        PassBid bid2 = new PassBid(testPlayerId);

        assertEquals(bid1, bid2, "Two PassBids with the same PlayerId should be considered equal");
    }
}