package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.HumanStrategy;
import base.domain.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PassBidTest {

    private Player testPlayer;
    private PassBid bid;

    @BeforeEach
    void setUp() {
        testPlayer = new Player(new HumanStrategy(), "Passer");
        bid = new PassBid(testPlayer);
    }

    @Test
    void getPlayer_ReturnsPlayer() {
        assertEquals(testPlayer, bid.getPlayer());
    }

    @Test
    void getType_ReturnsPass() {
        assertEquals(BidType.PASS, bid.getType());
    }

    @Test
    void getChosenTrump_ReturnsNull() {
        // A PassBid never determines the trump suit, so it should always safely return null
        assertNull(bid.getChosenTrump(Suit.HEARTS));
        assertNull(bid.getChosenTrump(null));
    }

    @Test
    void calculateBasePoints_AlwaysReturnsPassPoints() {
        // A PassBid always awards its base points (which should be 0 in the BidType enum)
        // regardless of how many tricks the player accidentally wins during the round.
        int expected = BidType.PASS.getBasePoints();

        assertEquals(expected, bid.calculateBasePoints(0));
        assertEquals(expected, bid.calculateBasePoints(6));
        assertEquals(expected, bid.calculateBasePoints(13));
    }

    @Test
    void calculateBasePoints_NegativeInput_ThrowsException() {
        // Edge case: Even for a PassBid, the system should catch invalid negative tricks
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                bid.calculateBasePoints(-1)
        );
        assertTrue(exception.getMessage().contains("negative tricks"));
    }

    @Test
    void testRecordEquality() {
        // Records have built-in equals() and hashCode() methods based on their fields.
        // Testing this ensures the JVM handles identical PassBids correctly.
        PassBid bid1 = new PassBid(testPlayer);
        PassBid bid2 = new PassBid(testPlayer);

        assertEquals(bid1, bid2);
    }

    @Test
    void testRecordAccessor() {
        // Testing the native record accessor for 100% method coverage
        assertEquals(testPlayer, bid.player());
    }
}