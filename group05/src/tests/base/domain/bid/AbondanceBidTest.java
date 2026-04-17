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

@DisplayName("Abondance Bid Rules & Calculations")
class AbondanceBidTest {

    private PlayerId testPlayerId;
    private BidType abondanceBidType;
    private Suit chosenTrump;
    private AbondanceBid bid;

    @BeforeEach
    void setUp() {
        // Arrange
        testPlayerId = new PlayerId();
        abondanceBidType = BidType.ABONDANCE_9; // Assumes this belongs to BidCategory.ABONDANCE
        chosenTrump = Suit.SPADES;

        bid = new AbondanceBid(testPlayerId, abondanceBidType, chosenTrump);
    }

    @Test
    @DisplayName("Constructor enforces non-null parameters")
    void constructor_NullParameters_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                        new AbondanceBid(null, abondanceBidType, chosenTrump),
                "Should reject null PlayerId"
        );

        assertThrows(IllegalArgumentException.class, () ->
                        new AbondanceBid(testPlayerId, null, chosenTrump),
                "Should reject null BidType"
        );
    }

    @Test
    @DisplayName("Constructor rejects BidTypes outside the ABONDANCE category")
    void constructor_InvalidBidCategory_ThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new AbondanceBid(testPlayerId, BidType.MISERIE, chosenTrump)
        );
        assertTrue(exception.getMessage().contains("ABONDANCE rank"));
    }

    @Test
    @DisplayName("getTeam() returns only the solo bidder")
    void getTeam_AlwaysReturnsOnlyTheBidder() {
        // Act
        // Abondance ignores the allBids and allPlayers lists, so empty lists are safe to pass
        List<PlayerId> team = bid.getTeam(Collections.emptyList(), Collections.emptyList());

        // Assert
        assertEquals(1, team.size());
        assertTrue(team.contains(testPlayerId));
    }

    @Test
    @DisplayName("determineTrump() overrides dealt trump with the player's chosen suit")
    void determineTrump_ValidDealtTrump_ReturnsChosenTrump() {
        assertEquals(chosenTrump, bid.determineTrump(Suit.HEARTS));
    }

    @Test
    @DisplayName("determineTrump() rejects null dealt trump")
    void determineTrump_NullDealtTrump_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> bid.determineTrump(null));
    }

    @ParameterizedTest(name = "Winning with {0} tricks awards positive base points")
    @ValueSource(ints = {9, 10, 13}) // Testing exact target, over target, and max tricks
    void calculateBasePoints_TargetMetOrExceeded_ReturnsPositivePoints(int tricksWon) {
        // Arrange
        int expectedPoints = abondanceBidType.getBasePoints();

        // Act & Assert
        assertEquals(expectedPoints, bid.calculateBasePoints(tricksWon));
    }

    @ParameterizedTest(name = "Failing with {0} tricks deducts base points")
    @ValueSource(ints = {8, 4, 0}) // Testing barely failed, severely failed, and 0 tricks
    void calculateBasePoints_BelowTarget_ReturnsNegativePoints(int tricksWon) {
        // Arrange
        int expectedPenalty = -1 * abondanceBidType.getBasePoints();

        // Act & Assert
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

    @Test
    @DisplayName("Native record accessors return correct values")
    void testRecordAccessors() {
        assertEquals(testPlayerId, bid.playerId());
        assertEquals(abondanceBidType, bid.bidType());
        assertEquals(chosenTrump, bid.trump());
    }
}