package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.HumanStrategy;
import base.domain.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BidTest {

    private Player dummyPlayer;

    @BeforeEach
    void setUp() {
        dummyPlayer = new Player(new HumanStrategy(), "Dummy Player");
    }

    @Test
    void compareTo_OrdersByBidTypeEnumDeclaration() {
        // Create a lower bid (declared at the top of the BidType enum)
        Bid lowerBid = new PassBid(dummyPlayer);

        // Create a higher bid (declared at the bottom of the BidType enum)
        Bid higherBid = new SoloBid(dummyPlayer, BidType.SOLO, Suit.HEARTS);

        // Create an identical bid to test equality
        Bid equalBid = new PassBid(dummyPlayer);

        // Case 1: Lower vs Higher (Should return a negative integer)
        assertTrue(lowerBid.compareTo(higherBid) < 0,
                "A lower bid (PASS) compared to a higher bid (SOLO) should be < 0");

        // Case 2: Higher vs Lower (Should return a positive integer)
        assertTrue(higherBid.compareTo(lowerBid) > 0,
                "A higher bid (SOLO) compared to a lower bid (PASS) should be > 0");

        // Case 3: Equal Bids (Should return exactly 0)
        assertEquals(0, lowerBid.compareTo(equalBid),
                "Two bids of the exact same type should return 0");
    }
}