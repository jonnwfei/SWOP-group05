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
    void getPlayer_ReturnsPlayer() {
        assertEquals(testPlayer, bid.getPlayer());
    }

    @Test
    void getType_ReturnsSoloProposal() {
        assertEquals(BidType.SOLO_PROPOSAL, bid.getType());
    }

    @Test
    void getChosenTrump_ReturnsDealtTrump() {
        // A solo proposal uses the dealt trump suit, so it should just return what was dealt
        assertEquals(dealtTrump, bid.getChosenTrump(dealtTrump));
    }

    @Test
    void calculateBasePoints_Success_Normal() {
        int base = BidType.SOLO_PROPOSAL.getBasePoints();
        int target = BidType.SOLO_PROPOSAL.getTargetTricks();

        // Case 1: Exact target achieved
        assertEquals(base, bid.calculateBasePoints(target));

        // Case 2: Overtricks (but not all 13).
        // According to the implementation, overtricks don't award extra points unless it's a full slam.
        assertEquals(base, bid.calculateBasePoints(target + 1));
        assertEquals(base, bid.calculateBasePoints(12));
    }

    @Test
    void calculateBasePoints_Success_Slam() {
        int base = BidType.SOLO_PROPOSAL.getBasePoints();

        // Case 3: Taking all 13 tricks doubles the points!
        assertEquals(2 * base, bid.calculateBasePoints(13));
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