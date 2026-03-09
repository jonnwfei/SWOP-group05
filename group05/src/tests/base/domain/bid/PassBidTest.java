package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.HumanStrategy;
import base.domain.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PassBidTest {

    private Player testPlayer;

    @BeforeEach
    void setUp() {
        testPlayer = new Player(new HumanStrategy(), "Passer");
    }

    @Test
    void getPlayer() {
        PassBid bid = new PassBid(testPlayer);
        assertEquals(testPlayer, bid.getPlayer());
    }

    @Test
    void getType() {
        PassBid bid = new PassBid(testPlayer);
        assertEquals(BidType.PASS, bid.getType());
    }

    @Test
    void getChosenTrump_ReturnsNull() {
        PassBid bid = new PassBid(testPlayer);
        // Een PassBid mag nooit de troef bepalen
        assertNull(bid.getChosenTrump(Suit.HEARTS));
        assertNull(bid.getChosenTrump(null));
    }

    @Test
    void calculateBasePoints_AlwaysReturnsPassPoints() {
        PassBid bid = new PassBid(testPlayer);
        // TODO: Controleer of BidType.PASS.getBasePoints() inderdaad 0 is in je Enum
        int expected = BidType.PASS.getBasePoints();

        assertEquals(expected, bid.calculateBasePoints(0));
        assertEquals(expected, bid.calculateBasePoints(6));
        assertEquals(expected, bid.calculateBasePoints(13));
    }

    @Test
    void calculateBasePoints_NegativeInput_ThrowsException() {
        PassBid bid = new PassBid(testPlayer);
        assertThrows(IllegalArgumentException.class, () -> bid.calculateBasePoints(-1));
    }

    @Test
    void testRecordEquality() {
        // Records hebben automatische equals() methodes
        PassBid bid1 = new PassBid(testPlayer);
        PassBid bid2 = new PassBid(testPlayer);
        assertEquals(bid1, bid2);
    }
}