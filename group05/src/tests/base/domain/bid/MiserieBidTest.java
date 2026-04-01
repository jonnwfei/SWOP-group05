package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.HumanStrategy;
import base.domain.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MiserieBidTest {

    private Player testPlayer;
    private BidType miserieNormal;
    private MiserieBid bid;

    @BeforeEach
    void setUp() {
        testPlayer = new Player(new HumanStrategy(), "Jane Doe");
        // Assuming BidType.MISERIE exists and falls under BidCategory.MISERIE
        miserieNormal = BidType.MISERIE;
        bid = new MiserieBid(testPlayer, miserieNormal);
    }

    @Test
    void constructor_InvalidCategory_ThrowsException() {
        // Enforce GRASP invariant: Cannot instantiate a MiserieBid with a non-Miserie BidType
        // Assuming ABONDANCE_9 belongs to BidCategory.ABONDANCE, not MISERIE
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new MiserieBid(testPlayer, BidType.ABONDANCE_9)
        );
        assertTrue(exception.getMessage().contains("MISERIE category"));
    }

    @Test
    void constructor_NullPlayer_ThrowsException() {
        // Enforce GRASP invariant: Cannot instantiate a bid without a valid player
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new MiserieBid(null, BidType.MISERIE)
        );
    }

    @Test
    void constructor_NullBidType_ThrowsException() {
        // Enforce GRASP invariant: Cannot instantiate a bid without a valid player
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new MiserieBid(testPlayer, null)
        );
    }

    @Test
    void getPlayer_ReturnsPlayer() {
        assertEquals(testPlayer, bid.getPlayer());
    }

    @Test
    void getType_ReturnsBidType() {
        assertEquals(miserieNormal, bid.getType());
    }

    @Test
    void getChosenTrump_AlwaysReturnsNull() {
        // Miserie is always played "Sans Atout" (No Trumps), so this must explicitly return null
        assertNull(bid.getChosenTrump(Suit.SPADES));
        assertNull(bid.getChosenTrump(null));
    }

    @Test
    void calculateBasePoints_Success_ZeroTricks() {
        int expectedPoints = miserieNormal.getBasePoints();
        int target = miserieNormal.getTargetTricks(); // Should evaluate to 0

        // Case 1: Achieved exactly the target tricks (0 tricks for a perfect Miserie)
        assertEquals(expectedPoints, bid.calculateBasePoints(target));
    }

    @Test
    void calculateBasePoints_Failure_OneOrMoreTricks() {
        int negativePoints = -1 * miserieNormal.getBasePoints();
        int target = miserieNormal.getTargetTricks();

        // Case 2: Took more tricks than the target (Miserie contract broken)
        assertEquals(negativePoints, bid.calculateBasePoints(target + 1));

        // Extreme case: Took all 13 tricks
        assertEquals(negativePoints, bid.calculateBasePoints(13));
    }

    @Test
    void calculateBasePoints_NegativeInput_ThrowsException() {
        // Edge case: tricks won can never be mathematically negative
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                bid.calculateBasePoints(-1)
        );
        assertTrue(exception.getMessage().contains("negative tricks won"));
    }

    @Test
    void testRecordAccessors() {
        // Testing native record accessors for 100% method coverage
        assertEquals(testPlayer, bid.player());
        assertEquals(miserieNormal, bid.bidType());
    }
}