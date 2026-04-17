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

    private PlayerId testPlayerId;
    private PlayerId partnerId;

    @BeforeEach
    void setUp() {
        testPlayerId = new PlayerId();
        partnerId = new PlayerId();
    }

    @Test
    @DisplayName("Constructor enforces non-null parameters and correct categories")
    void constructor_InvalidParameters_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                        new TroelBid(null, BidType.TROEL, Suit.SPADES),
                "Should reject null PlayerId"
        );

        assertThrows(IllegalArgumentException.class, () ->
                        new TroelBid(testPlayerId, null, Suit.SPADES),
                "Should reject null BidType"
        );

        assertThrows(IllegalArgumentException.class, () ->
                        new TroelBid(testPlayerId, BidType.SOLO, Suit.SPADES),
                "Should reject BidTypes outside the TROEL category"
        );
    }

    @Test
    @DisplayName("TROEL requires an explicit missing Ace suit")
    void constructor_TroelNullSuit_ThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new TroelBid(testPlayerId, BidType.TROEL, null)
        );
        assertTrue(exception.getMessage().contains("requires the suit of the missing Ace"));
    }

    @Test
    @DisplayName("TROELA automatically overrides trump suit to Hearts")
    void constructor_Troela_AlwaysSetsTrumpToHearts() {
        // Even if we pass null or SPADES, TROELA always forces Hearts
        TroelBid bid = new TroelBid(testPlayerId, BidType.TROELA, Suit.SPADES);
        assertEquals(Suit.HEARTS, bid.determineTrump(Suit.CLUBS), "TROELA trump must always be Hearts");
    }

    @Test
    @DisplayName("Basic Accessors return correctly assigned values")
    void basicAccessors_ReturnExpectedValues() {
        TroelBid bid = new TroelBid(testPlayerId, BidType.TROEL, Suit.CLUBS);

        assertEquals(testPlayerId, bid.getPlayerId());
        assertEquals(testPlayerId, bid.playerId());
        assertEquals(BidType.TROEL, bid.getType());
        assertEquals(BidType.TROEL, bid.bidType());
        assertEquals(Suit.CLUBS, bid.trumpSuit());
        assertEquals(Suit.CLUBS, bid.determineTrump(Suit.HEARTS));
    }

    @Test
    @DisplayName("getTeam() for TROEL finds the player holding the missing Ace")
    void getTeam_Troel_FindsPartnerWithMissingAce() {
        TroelBid bid = new TroelBid(testPlayerId, BidType.TROEL, Suit.CLUBS);

        // Arrange Mocks
        Player mockBidder = mock(Player.class);
        when(mockBidder.getId()).thenReturn(testPlayerId);
        when(mockBidder.hasCard(any())).thenReturn(false);

        Player mockPartner = mock(Player.class);
        when(mockPartner.getId()).thenReturn(partnerId);
        // The partner holds the missing Ace of Clubs
        when(mockPartner.hasCard(new Card(Suit.CLUBS, Rank.ACE))).thenReturn(true);

        List<Player> allPlayers = List.of(mockBidder, mockPartner);

        // Act
        List<PlayerId> team = bid.getTeam(Collections.emptyList(), allPlayers);

        // Assert
        assertEquals(2, team.size());
        assertTrue(team.contains(testPlayerId));
        assertTrue(team.contains(partnerId));
    }

    @Test
    @DisplayName("getTeam() for TROELA finds the player with the highest Heart")
    void getTeam_Troela_FindsPartnerWithHighestHeart() {
        TroelBid bid = new TroelBid(testPlayerId, BidType.TROELA, null); // Trump is forced to HEARTS internally

        // Arrange Mocks
        Player mockBidder = mock(Player.class);
        when(mockBidder.getId()).thenReturn(testPlayerId);
        // Bidder's hand is ignored by the algorithm because they cannot be their own partner

        Player mockBystander = mock(Player.class);
        when(mockBystander.getId()).thenReturn(new PlayerId());
        when(mockBystander.getHand()).thenReturn(List.of(new Card(Suit.HEARTS, Rank.TWO)));

        Player mockPartner = mock(Player.class);
        when(mockPartner.getId()).thenReturn(partnerId);
        // Partner holds the King of Hearts, which beats the bystander's Two of Hearts
        when(mockPartner.getHand()).thenReturn(List.of(new Card(Suit.HEARTS, Rank.KING)));

        List<Player> allPlayers = List.of(mockBidder, mockBystander, mockPartner);

        // Act
        List<PlayerId> team = bid.getTeam(Collections.emptyList(), allPlayers);

        // Assert
        assertEquals(2, team.size());
        assertEquals(testPlayerId, team.get(0)); // Bidder first
        assertEquals(partnerId, team.get(1));    // Partner second
    }

    @Test
    @DisplayName("getTeam() throws Exception if corrupted deck prevents finding a partner")
    void getTeam_PartnerNotFound_ThrowsIllegalStateException() {
        TroelBid bid = new TroelBid(testPlayerId, BidType.TROEL, Suit.CLUBS);

        // Create a game where NO ONE has the missing Ace
        Player mockBidder = mock(Player.class);
        when(mockBidder.getId()).thenReturn(testPlayerId);
        when(mockBidder.hasCard(any())).thenReturn(false);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                bid.getTeam(Collections.emptyList(), List.of(mockBidder))
        );
        assertTrue(exception.getMessage().contains("Corrupted deck"));
    }

    @ParameterizedTest(name = "TROEL (Target 8) - Winning {0} tricks yields {1} points (Base 4 + 2/overtrick)")
    @CsvSource({
            "8, 4",    // Exact target
            "10, 8",   // 2 Overtricks -> 4 + (2 * 2) = 8
    })
    void calculateBasePoints_TroelSuccess_ReturnsPoints(int tricksWon, int expectedPoints) {
        TroelBid bid = new TroelBid(testPlayerId, BidType.TROEL, Suit.CLUBS);
        assertEquals(expectedPoints, bid.calculateBasePoints(tricksWon));
    }

    @ParameterizedTest(name = "TROELA (Target 9) - Winning {0} tricks yields {1} points (Base 4 + 2/overtrick)")
    @CsvSource({
            "9, 4",    // Exact target
            "11, 8",   // 2 Overtricks -> 4 + (2 * 2) = 8
    })
    void calculateBasePoints_TroelaSuccess_ReturnsPoints(int tricksWon, int expectedPoints) {
        TroelBid bid = new TroelBid(testPlayerId, BidType.TROELA, Suit.HEARTS);
        assertEquals(expectedPoints, bid.calculateBasePoints(tricksWon));
    }

    @Test
    @DisplayName("Winning all 13 tricks (Slam) doubles the total accumulated points")
    void calculateBasePoints_SlamAchieved_DoublesTotalPoints() {
        TroelBid bid = new TroelBid(testPlayerId, BidType.TROEL, Suit.CLUBS);

        // Base(4) + 5 Overtricks(10) = 14 points. Doubled for Slam = 28 points.
        assertEquals(28, bid.calculateBasePoints(13));
    }

    @ParameterizedTest(name = "Failing contract yields negative base points (-4)")
    @ValueSource(ints = {7, 4, 0})
    void calculateBasePoints_BelowTarget_ReturnsNegativeBasePoints(int tricksWon) {
        TroelBid bid = new TroelBid(testPlayerId, BidType.TROEL, Suit.CLUBS);
        int expectedPenalty = -1 * BidType.TROEL.getBasePoints();
        assertEquals(expectedPenalty, bid.calculateBasePoints(tricksWon));
    }

    @Test
    @DisplayName("calculateBasePoints() rejects negative trick counts")
    void calculateBasePoints_NegativeInput_ThrowsIllegalArgumentException() {
        TroelBid bid = new TroelBid(testPlayerId, BidType.TROEL, Suit.CLUBS);
        assertThrows(IllegalArgumentException.class, () -> bid.calculateBasePoints(-1));
    }
}