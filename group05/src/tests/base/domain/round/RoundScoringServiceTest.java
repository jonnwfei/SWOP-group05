package base.domain.round;

import base.domain.bid.BidType;
import base.domain.bid.MiserieBid;
import base.domain.bid.SoloBid;
import base.domain.card.Suit;
import base.domain.player.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("RoundScoringService")
class RoundScoringServiceTest {

    @Mock private Round round;
    @Mock private Player p1;
    @Mock private Player p2;
    @Mock private Player p3;
    @Mock private Player p4;

    private AutoCloseable mocks;
    private RoundScoringService service;
    private List<Player> allPlayers;

    // Real bid instances — Bid is a sealed interface; all permits are records.
    private static final SoloBid  SOLO_BID    = new SoloBid(BidType.SOLO, Suit.HEARTS);
    private static final MiserieBid MISERIE_BID = new MiserieBid(BidType.MISERIE);

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new RoundScoringService();
        allPlayers = List.of(p1, p2, p3, p4);

        lenient().when(round.getHighestBid()).thenReturn(SOLO_BID);
        lenient().when(round.getPlayers()).thenReturn(allPlayers);
        lenient().when(round.getMultiplier()).thenReturn(1);
        lenient().when(round.getTricks()).thenReturn(List.of());
        lenient().when(round.getCountTricksWon()).thenReturn(-1);
        lenient().when(round.getCountMiserieWinners()).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() throws Exception { mocks.close(); }

    // =========================================================================
    // calculateScores — guard
    // =========================================================================

    @Nested
    @DisplayName("calculateScores — guard")
    class GuardTests {

        @Test
        @DisplayName("Throws IllegalStateException when round has no highest bid")
        void throwsWhenNoBid() {
            when(round.getHighestBid()).thenReturn(null);
            assertThrows(IllegalStateException.class, () -> service.calculateScores(round));
        }
    }

    // =========================================================================
    // calculateScores — standard bid
    //
    // Uses SoloBid(SOLO, HEARTS): targetTricks=13, basePoints=75.
    // With countTricksWon=13 → calculateBasePoints(13) = 75 (success).
    // =========================================================================

    @Nested
    @DisplayName("calculateScores — standard (non-miserie)")
    class StandardBidTests {

        @BeforeEach
        void setUpSoloBid() {
            when(round.getHighestBid()).thenReturn(SOLO_BID);
            when(round.getBiddingTeamPlayers()).thenReturn(List.of(p1));
            // 13 tricks = SOLO target → calculateBasePoints(13) = +75
            when(round.getCountTricksWon()).thenReturn(13);
        }

        @Test
        @DisplayName("Calls markFinished() after calculating scores")
        void callsMarkFinished() {
            service.calculateScores(round);
            verify(round).markFinished();
        }

        @Test
        @DisplayName("Delegates to distributeScores with correct basePoints")
        void distributesCorrectPoints() {
            service.calculateScores(round);
            // p1 is the sole bidder (1v3), base = 75, multiplier = 1
            verify(round).addScoreDelta(0, 75);
            verify(round).addScoreDelta(1, -25);
            verify(round).addScoreDelta(2, -25);
            verify(round).addScoreDelta(3, -25);
            verify(p1).updateScore(75);
            verify(p2).updateScore(-25);
            verify(p3).updateScore(-25);
            verify(p4).updateScore(-25);
        }
    }

    // =========================================================================
    // calculateScores — miserie bid
    //
    // Uses MiserieBid(MISERIE): targetTricks=0, basePoints=21.
    // Count-mode: tricks list is empty, getCountMiserieWinners() = [] (p1 not a winner)
    // → resolvePlayerTricks returns 1.  calculateBasePoints(1): 1 > 0 → -21 (failure).
    // =========================================================================

    @Nested
    @DisplayName("calculateScores — miserie bid")
    class MiserieBidTests {

        @BeforeEach
        void setUpMiserieBid() {
            when(round.getHighestBid()).thenReturn(MISERIE_BID);
            when(round.getBiddingTeamPlayers()).thenReturn(List.of(p1));
            // Empty tricks → count-mode; p1 not in miserieWinners → 1 trick taken (failure)
            when(round.getTricks()).thenReturn(List.of());
            when(round.getCountMiserieWinners()).thenReturn(List.of());
        }

        @Test
        @DisplayName("Calls markFinished() after miserie scoring")
        void callsMarkFinished() {
            service.calculateScores(round);
            verify(round).markFinished();
        }

        @Test
        @DisplayName("Score is distributed per miserie bidder individually")
        void perBidderDistribution() {
            service.calculateScores(round);
            // p1 failed (1 trick taken): base = -21, 1v3 → p1: -21, others: +7 each
            verify(p1).updateScore(-21);
            verify(p2).updateScore(7);
            verify(p3).updateScore(7);
            verify(p4).updateScore(7);
        }
    }

    // =========================================================================
    // distributeScores — 1 vs 3 (solo)
    // =========================================================================

    @Nested
    @DisplayName("distributeScores — 1 bidder (solo)")
    class OneBidderTests {

        @Test
        @DisplayName("Bidder gets +points, three others share the loss equally")
        void zeroSumSolo() {
            service.distributeScores(round, 75, List.of(p1));

            verify(p1).updateScore(75);
            verify(p2).updateScore(-25);
            verify(p3).updateScore(-25);
            verify(p4).updateScore(-25);

            verify(round).addScoreDelta(0, 75);
            verify(round).addScoreDelta(1, -25);
            verify(round).addScoreDelta(2, -25);
            verify(round).addScoreDelta(3, -25);
        }

        @Test
        @DisplayName("Throws when points not divisible by 3 for 1v3 round")
        void throwsWhenNotDivisibleByThree() {
            assertThrows(IllegalStateException.class,
                    () -> service.distributeScores(round, 10, List.of(p1)));
        }

        @Test
        @DisplayName("Negative base points: bidder loses, others gain")
        void negativePointsSolo() {
            service.distributeScores(round, -21, List.of(p1));
            verify(p1).updateScore(-21);
            verify(p2).updateScore(7);
            verify(p3).updateScore(7);
            verify(p4).updateScore(7);
        }
    }

    // =========================================================================
    // distributeScores — 2 vs 2 (proposal/acceptance)
    // =========================================================================

    @Nested
    @DisplayName("distributeScores — 2 bidders (proposal)")
    class TwoBidderTests {

        @Test
        @DisplayName("Each bidder gets +points, each non-bidder gets -points")
        void zeroSumProposal() {
            service.distributeScores(round, 8, List.of(p1, p2));

            verify(p1).updateScore(8);
            verify(p2).updateScore(8);
            verify(p3).updateScore(-8);
            verify(p4).updateScore(-8);
        }

        @Test
        @DisplayName("Multiplier is applied to all deltas")
        void multiplierApplied() {
            when(round.getMultiplier()).thenReturn(2);
            service.distributeScores(round, 8, List.of(p1, p2));

            verify(p1).updateScore(16);
            verify(p2).updateScore(16);
            verify(p3).updateScore(-16);
            verify(p4).updateScore(-16);
        }
    }
}
