package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.HumanStrategy;
import base.domain.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AbondanceBidTest {

    private Player testPlayer;
    private BidType abondanceBidType;
    private Suit testTrump;
    private AbondanceBid bid;

    @BeforeEach
    void setUp() {
        testPlayer = new Player(new HumanStrategy(), "Jane Doe");
        // Assuming BidType.ABONDANCE_9 exists and falls under BidCategory.ABONDANCE
        abondanceBidType = BidType.ABONDANCE_9;
        testTrump = Suit.SPADES;

        // Instantiate once here to keep the test methods DRY
        bid = new AbondanceBid(testPlayer, abondanceBidType, testTrump);
    }

    @Test
    void constructor_InvalidCategory_ThrowsException() {
        // Enforce GRASP invariant: Cannot instantiate an AbondanceBid with a non-Abondance BidType
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new AbondanceBid(testPlayer, BidType.MISERIE, testTrump)
        );
        assertTrue(exception.getMessage().contains("ABONDANCE rank"));
    }

    @Test
    void getPlayer_ReturnsPlayer() {
        assertEquals(testPlayer, bid.getPlayer());
    }

    @Test
    void getType_ReturnsBidType() {
        assertEquals(abondanceBidType, bid.getType());
    }

    @Test
    void getChosenTrump_ReturnsSetTrump() {
        // For an Abondance bid, the dealt trump is ignored; the player's chosen trump always applies
        assertEquals(testTrump, bid.getChosenTrump(Suit.HEARTS));
    }

    @Test
    void calculateBasePoints_Success() {
        int expectedPoints = abondanceBidType.getBasePoints();
        int target = abondanceBidType.getTargetTricks();

        // Case 1: Player gets exactly the target amount of tricks
        assertEquals(expectedPoints, bid.calculateBasePoints(target));

        // Case 2: Player gets more tricks than required
        assertEquals(expectedPoints, bid.calculateBasePoints(target + 1));

        // Case 3: Player takes all 13 tricks
        // (Standard Abondance math doesn't multiply points here unless explicitly coded)
        assertEquals(expectedPoints, bid.calculateBasePoints(13));
    }

    @Test
    void calculateBasePoints_Failure() {
        int negativePoints = -1 * abondanceBidType.getBasePoints();
        int target = abondanceBidType.getTargetTricks();

        // Failed by exactly 1 trick
        assertEquals(negativePoints, bid.calculateBasePoints(target - 1));

        // Failed completely (got 0 tricks)
        assertEquals(negativePoints, bid.calculateBasePoints(0));
    }

    @Test
    void calculateBasePoints_NegativeInput_ThrowsException() {
        // Edge case: tricks won can never be physically negative
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                bid.calculateBasePoints(-1)
        );
        assertTrue(exception.getMessage().contains("negative tricks won"));
    }

    @Test
    void testRecordAccessors() {
        // Testing native record accessors for 100% method and branch coverage
        assertEquals(testPlayer, bid.player());
        assertEquals(abondanceBidType, bid.bidType());
        assertEquals(testTrump, bid.trump());
    }
}