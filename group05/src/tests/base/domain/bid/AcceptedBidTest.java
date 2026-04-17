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

@DisplayName("Accepted Bid Rules & Calculations")
class AcceptedBidTest {

    private PlayerId acceptorId;
    private Suit dealtTrump;
    private AcceptedBid bid;

    @BeforeEach
    void setUp() {
        // Arrange
        acceptorId = new PlayerId();
        dealtTrump = Suit.CLUBS;
        bid = new AcceptedBid(acceptorId);
    }

    @Test
    @DisplayName("Constructor enforces non-null parameters")
    void constructor_NullParameters_ThrowsIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new AcceptedBid(null)
        );
        assertTrue(exception.getMessage().contains("null"), "Should reject null Acceptor PlayerId");
    }

    @Test
    @DisplayName("Basic Accessors return correctly assigned values")
    void basicAccessors_ReturnExpectedValues() {
        assertEquals(acceptorId, bid.getPlayerId(), "getPlayerId() should return the acceptor's ID");
        assertEquals(acceptorId, bid.acceptor(), "Record accessor should return the acceptor's ID");
        assertEquals(BidType.ACCEPTANCE, bid.getType(), "getType() should always return ACCEPTANCE");
    }

    @Test
    @DisplayName("determineTrump() enforces the originally dealt trump suit")
    void determineTrump_ValidDealtTrump_ReturnsDealtTrump() {
        assertEquals(dealtTrump, bid.determineTrump(dealtTrump));
        assertEquals(Suit.DIAMONDS, bid.determineTrump(Suit.DIAMONDS));
    }

    @Test
    @DisplayName("determineTrump() rejects null dealt trump")
    void determineTrump_NullDealtTrump_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> bid.determineTrump(null));
    }

    @Test
    @DisplayName("getTeam() successfully finds the proposer and pairs them with the acceptor")
    void getTeam_ProposalExists_ReturnsAcceptorAndProposer() {
        // Arrange
        PlayerId proposerId = new PlayerId();

        // Mocking the bid history to isolate this test from other Bid implementations
        Bid mockProposal = mock(ProposalBid.class);
        when(mockProposal.getType()).thenReturn(BidType.PROPOSAL);
        when(mockProposal.getPlayerId()).thenReturn(proposerId);

        Bid mockPass = mock(PassBid.class);
        when(mockPass.getType()).thenReturn(BidType.PASS);

        List<Bid> bidHistory = List.of(mockPass, mockProposal, bid);
        List<Player> allPlayers = Collections.emptyList(); // Not used by AcceptedBid

        // Act
        List<PlayerId> team = bid.getTeam(bidHistory, allPlayers);

        // Assert
        assertEquals(2, team.size(), "Team should consist of exactly 2 players");
        assertTrue(team.contains(acceptorId), "Team must contain the acceptor");
        assertTrue(team.contains(proposerId), "Team must contain the original proposer");
    }

    @Test
    @DisplayName("getTeam() throws an exception if no Proposal bid exists in history")
    void getTeam_NoProposalExists_ThrowsIllegalArgumentException() {
        // Arrange: A bid history with only PASS bids
        Bid mockPass = mock(PassBid.class);
        when(mockPass.getType()).thenReturn(BidType.PASS);

        List<Bid> bidHistory = List.of(mockPass, mockPass, bid);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                bid.getTeam(bidHistory, Collections.emptyList())
        );
        assertTrue(exception.getMessage().contains("impossible to have AcceptedBid without ProposalBid"));
    }

    @ParameterizedTest(name = "Winning {0} tricks yields {1} base points (including overtricks)")
    @CsvSource({
            "8, 2",   // Target met exactly
            "9, 3",   // 1 Overtrick
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
        // When failing an acceptance, overtricks are ignored and the flat base penalty is applied (-2)
        int expectedPenalty = -1 * BidType.ACCEPTANCE.getBasePoints();
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