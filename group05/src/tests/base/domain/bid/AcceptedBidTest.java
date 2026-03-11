package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.HumanStrategy;
import base.domain.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AcceptedBidTest {

    private Player acceptor;
    private Suit dealtTrump;
    private AcceptedBid bid;

    @BeforeEach
    void setUp() {
        acceptor = new Player(new HumanStrategy(), "Partner");
        dealtTrump = Suit.CLUBS;
        bid = new AcceptedBid(acceptor);
    }

    @Test
    void getPlayer_ReturnsPlayer() {
        assertEquals(acceptor, bid.getPlayer());
    }

    @Test
    void getType_ReturnsAcceptance() {
        assertEquals(BidType.ACCEPTANCE, bid.getType());
    }

    @Test
    void getChosenTrump_ReturnsDealtTrump() {
        // An accepted bid always plays with the dealt trump suit, so it should return the input unchanged
        assertEquals(dealtTrump, bid.getChosenTrump(dealtTrump));
        assertEquals(Suit.DIAMONDS, bid.getChosenTrump(Suit.DIAMONDS));
    }

    @Test
    void calculateBasePoints_Success_Normal() {
        int base = BidType.ACCEPTANCE.getBasePoints();
        int target = BidType.ACCEPTANCE.getTargetTricks();

        // Case 1: Exact target achieved
        assertEquals(base, bid.calculateBasePoints(target));

        // Case 2: Overtricks (but not a full slam).
        // Extra points for overtricks are usually calculated at the round level,
        // the bid itself just validates the base points.
        assertEquals(base, bid.calculateBasePoints(target + 1));
        assertEquals(base, bid.calculateBasePoints(12));
    }

    @Test
    void calculateBasePoints_Failure() {
        int base = BidType.ACCEPTANCE.getBasePoints();
        int target = BidType.ACCEPTANCE.getTargetTricks();

        // Failed by exactly 1 trick (should return negative base points)
        assertEquals(-base, bid.calculateBasePoints(target - 1));

        // Failed completely (got 0 tricks)
        assertEquals(-base, bid.calculateBasePoints(0));
    }

    @Test
    void calculateBasePoints_Success_Slam() {
        int base = BidType.ACCEPTANCE.getBasePoints();

        // Case 3: Taking all 13 tricks doubles the points!
        assertEquals(2 * base, bid.calculateBasePoints(13));
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
        assertEquals(acceptor, bid.acceptor());
    }
}