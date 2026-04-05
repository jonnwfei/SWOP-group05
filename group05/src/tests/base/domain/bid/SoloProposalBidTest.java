package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.HumanStrategy;
import base.domain.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SoloProposalBidTest {

    private Player testPlayer;
    private Suit dealtTrump;
    private SoloProposalBid bid;

    @BeforeEach
    void setUp() {
        testPlayer = new Player(new HumanStrategy(), "Soloist");
        dealtTrump = Suit.DIAMONDS;
        bid = new SoloProposalBid(testPlayer);
    }

    @Test
    void constructor_NullPlayer_ThrowsException() {
        // Enforce GRASP invariant: Cannot instantiate a bid without a valid player
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new SoloProposalBid(null)
        );
    }

    @Test
    void getPlayer_ReturnsPlayer() {
        assertEquals(testPlayer, bid.getPlayer());
    }

    @Test
    void getType_ReturnsSoloProposal() {
        assertEquals(BidType.SOLO_PROPOSAL, bid.getType());
    }

    @Test
    void determineTrump_NullDealtTrump_ThrowsException() {
        // Enforce GRASP invariant: A Proposal bid relies on the dealt trump, which cannot be null.
        Player testPlayer = new Player(new HumanStrategy(), "Proposer");
        SoloProposalBid bid = new SoloProposalBid(testPlayer);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                bid.determineTrump(null)
        );

        assertTrue(exception.getMessage().toLowerCase().contains("null"),
                "Exception message should mention null.");
    }

    @Test
    void determineTrump_ReturnsDealtTrump() {
        // A solo proposal uses the dealt trump suit, so it should just return what was dealt
        assertEquals(dealtTrump, bid.determineTrump(dealtTrump));
    }

    @Test
    void calculateBasePoints_Success_Normal() {
        int base = BidType.SOLO_PROPOSAL.getBasePoints();     // 6
        int target = BidType.SOLO_PROPOSAL.getTargetTricks(); // 5

        // Case 1: Exact target achieved (5 tricks = 6 points)
        assertEquals(base, bid.calculateBasePoints(target));

        // Case 2: Overtricks (but not all 13).
        // +3 points for each excess trick
        assertEquals(base + 3, bid.calculateBasePoints(target + 1)); // 6 tricks = 9 points
        assertEquals(base + (3 * 7), bid.calculateBasePoints(12));   // 12 tricks = 6 + 21 = 27 points
    }

    @Test
    void calculateBasePoints_Success_Slam() {
        int base = BidType.SOLO_PROPOSAL.getBasePoints();     // 6
        int target = BidType.SOLO_PROPOSAL.getTargetTricks(); // 5

        // Case 3: Taking all 13 tricks doubles the TOTAL points!
        int excessTricks = 13 - target; // 8
        int expectedBeforeDouble = base + (excessTricks * 3); // 6 + 24 = 30
        int expectedSlamPoints = expectedBeforeDouble * 2;    // 60

        assertEquals(expectedSlamPoints, bid.calculateBasePoints(13));
    }

    @Test
    void calculateBasePoints_Failure() {
        int base = BidType.SOLO_PROPOSAL.getBasePoints();
        int target = BidType.SOLO_PROPOSAL.getTargetTricks();

        // Failed by exactly 1 trick (should return negative base points)
        assertEquals(-base, bid.calculateBasePoints(target - 1));

        // Failed completely (got 0 tricks)
        assertEquals(-base, bid.calculateBasePoints(0));
    }

    @Test
    void calculateBasePoints_NegativeInput_ThrowsException() {
        // Edge case: tricks won can never be negative
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                bid.calculateBasePoints(-1)
        );
        assertTrue(exception.getMessage().contains("negative tricks won"));
    }

    @Test
    void testRecordAccessor() {
        // Testing the native record accessor for 100% method coverage
        assertEquals(testPlayer, bid.player());
    }
}