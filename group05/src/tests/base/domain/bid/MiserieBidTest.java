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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Miserie Bid Rules & Calculations")
class MiserieBidTest {

    private PlayerId testPlayerId;
    private BidType miserieBidType;
    private MiserieBid bid;

    @BeforeEach
    void setUp() {
        // Arrange
        testPlayerId = new PlayerId("player-123");
        miserieBidType = BidType.MISERIE;
        bid = new MiserieBid(testPlayerId, miserieBidType);
    }

    @Test
    @DisplayName("Constructor enforces non-null parameters")
    void constructor_NullParameters_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                        new MiserieBid(null, miserieBidType),
                "Should reject null PlayerId"
        );

        assertThrows(IllegalArgumentException.class, () ->
                        new MiserieBid(testPlayerId, null),
                "Should reject null BidType"
        );
    }

    @Test
    @DisplayName("Constructor rejects BidTypes outside the MISERIE category")
    void constructor_InvalidBidCategory_ThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new MiserieBid(testPlayerId, BidType.ABONDANCE_9)
        );
        assertTrue(exception.getMessage().contains("MISERIE category"));
    }

    @Test
    @DisplayName("Basic Accessors return correctly assigned values")
    void basicAccessors_ReturnExpectedValues() {
        assertEquals(testPlayerId, bid.getPlayerId(), "getPlayerId() should return the player's ID");
        assertEquals(testPlayerId, bid.playerId(), "Record accessor should return the player's ID");
        assertEquals(miserieBidType, bid.getType(), "getType() should return the assigned BidType");
        assertEquals(miserieBidType, bid.bidType(), "Record accessor should return the assigned BidType");
    }

    @Test
    @DisplayName("determineTrump() always returns null (No Trumps)")
    void determineTrump_AlwaysReturnsNull() {
        assertNull(bid.determineTrump(Suit.SPADES), "Miserie ignores dealt trump and returns null");
        assertNull(bid.determineTrump(null), "Miserie returns null even if dealt trump is null");
    }

    @Test
    @DisplayName("getTeam() returns all players who played this exact type of Miserie")
    void getTeam_MultipleMiserieBids_ReturnsAllMatchingPlayerIds() {
        // Arrange
        PlayerId player2Id = new PlayerId("player-456");
        PlayerId player3Id = new PlayerId("player-789");

        // Mock 1: Another normal Miserie bid
        Bid mockMiserie = mock(MiserieBid.class);
        when(mockMiserie.getType()).thenReturn(BidType.MISERIE);
        when(mockMiserie.getPlayerId()).thenReturn(player2Id);

        // Mock 2: An OPEN Miserie bid (should NOT be grouped with normal Miserie)
        Bid mockOpenMiserie = mock(MiserieBid.class);
        when(mockOpenMiserie.getType()).thenReturn(BidType.OPEN_MISERIE);
        when(mockOpenMiserie.getPlayerId()).thenReturn(player3Id);

        // Mock 3: A PASS bid
        Bid mockPass = mock(PassBid.class);
        when(mockPass.getType()).thenReturn(BidType.PASS);

        List<Bid> bidHistory = List.of(mockPass, mockMiserie, mockOpenMiserie, bid);

        // Act
        List<PlayerId> team = bid.getTeam(bidHistory, Collections.emptyList());

        // Assert
        assertEquals(2, team.size(), "Should only group players playing normal MISERIE");
        assertTrue(team.contains(testPlayerId), "Team should contain the primary bidder");
        assertTrue(team.contains(player2Id), "Team should contain the other Miserie bidder");
        assertFalse(team.contains(player3Id), "Team should NOT contain the Open Miserie bidder");
    }

    @Test
    @DisplayName("Winning the contract (0 tricks) yields positive base points")
    void calculateBasePoints_ZeroTricks_ReturnsPositivePoints() {
        int expectedPoints = miserieBidType.getBasePoints();
        assertEquals(expectedPoints, bid.calculateBasePoints(0));
    }

    @ParameterizedTest(name = "Failing contract by taking {0} tricks yields negative base points")
    @ValueSource(ints = {1, 2, 7, 13})
    void calculateBasePoints_OneOrMoreTricks_ReturnsNegativePoints(int tricksWon) {
        int expectedPenalty = -1 * miserieBidType.getBasePoints();
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