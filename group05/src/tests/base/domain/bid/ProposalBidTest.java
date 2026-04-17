package base.domain.bid;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Proposal Bid Rules & Calculations")
class ProposalBidTest {

    private PlayerId proposerId;
    private Suit dealtTrump;
    private ProposalBid bid;

    @BeforeEach
    void setUp() {
        // Arrange
        proposerId = new PlayerId();
        dealtTrump = Suit.HEARTS;
        bid = new ProposalBid(proposerId);
    }

    @Test
    @DisplayName("Constructor enforces non-null parameters")
    void constructor_NullParameters_ThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new ProposalBid(null)
        );
        assertTrue(exception.getMessage().toLowerCase().contains("null"), "Should reject null Proposer PlayerId");
    }

    @Test
    @DisplayName("Basic Accessors return correctly assigned values")
    void basicAccessors_ReturnExpectedValues() {
        assertEquals(proposerId, bid.getPlayerId(), "getPlayerId() should return the proposer's ID");
        assertEquals(proposerId, bid.proposer(), "Record accessor should return the proposer's ID");
        assertEquals(BidType.PROPOSAL, bid.getType(), "getType() should always return PROPOSAL");
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

    @Test
    @DisplayName("getTeam() successfully finds the acceptor and pairs them with the proposer")
    void getTeam_AcceptanceExists_ReturnsProposerAndAcceptor() {
        // Arrange
        PlayerId acceptorId = new PlayerId();

        // Mocking the bid history to isolate this test from the concrete AcceptedBid class
        Bid mockAcceptance = mock(Bid.class);
        when(mockAcceptance.getType()).thenReturn(BidType.ACCEPTANCE);
        when(mockAcceptance.getPlayerId()).thenReturn(acceptorId);

        Bid mockPass = mock(Bid.class);
        when(mockPass.getType()).thenReturn(BidType.PASS);

        List<Bid> bidHistory = List.of(bid, mockPass, mockAcceptance);
        List<Player> allPlayers = Collections.emptyList(); // Not used by ProposalBid

        // Act
        List<PlayerId> team = bid.getTeam(bidHistory, allPlayers);

        // Assert
        assertEquals(2, team.size(), "Team should consist of exactly 2 players");
        assertEquals(proposerId, team.get(0), "Team must contain the original proposer first");
        assertEquals(acceptorId, team.get(1), "Team must contain the acceptor second");
    }

    @Test
    @DisplayName("getTeam() throws an exception if no Acceptance bid exists in history")
    void getTeam_NoAcceptanceExists_ThrowsIllegalArgumentException() {
        // Arrange: A bid history with only PASS bids and the Proposal itself
        Bid mockPass = mock(Bid.class);
        when(mockPass.getType()).thenReturn(BidType.PASS);

        List<Bid> bidHistory = List.of(bid, mockPass, mockPass, mockPass);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                bid.getTeam(bidHistory, Collections.emptyList())
        );
        assertTrue(exception.getMessage().contains("impossible to have ProposalBid without AcceptedBid"));
    }

    @ParameterizedTest(name = "Winning {0} tricks yields {1} base points (including overtricks)")
    @CsvSource({
            "8, 2",   // Target met exactly
            "10, 4",  // 2 Overtricks
            "12, 6"   // 4 Overtricks
    })
    void calculateBasePoints_TargetMet_ReturnsBasePlusOvertricks(int tricksWon, int expectedPoints) {
        assertEquals(expectedPoints, bid.calculateBasePoints(tricksWon));
    }

    @Test
    @DisplayName("Winning all 13 tricks (Slam) doubles the total accumulated points")
    void calculateBasePoints_SlamAchieved_DoublesTotalPoints() {
        // Base(2) + Overtricks(5) = 7. Doubled for Slam = 14.
        assertEquals(14, bid.calculateBasePoints(13));
    }

    @ParameterizedTest(name = "Failing contract by taking only {0} tricks yields negative base points")
    @ValueSource(ints = {7, 4, 0})
    void calculateBasePoints_BelowTarget_ReturnsNegativeBasePoints(int tricksWon) {
        // When failing a proposal, overtricks logic is skipped and the flat base penalty is applied (-2)
        int expectedPenalty = -1 * BidType.ACCEPTANCE.getBasePoints();
        assertEquals(expectedPenalty, bid.calculateBasePoints(tricksWon));
    }

    @ParameterizedTest(name = "calculateBasePoints() rejects out-of-bounds trick count: {0}")
    @ValueSource(ints = {-1, 14, 100})
    void calculateBasePoints_OutOfBoundsInput_ThrowsIllegalArgumentException(int tricksWon) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                bid.calculateBasePoints(tricksWon)
        );
        assertTrue(exception.getMessage().contains("out of bound"));
    }
}