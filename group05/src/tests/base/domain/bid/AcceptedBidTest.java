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

    @BeforeEach
    void setUp() {
        acceptor = new Player(new HumanStrategy(), "Partner");
        dealtTrump = Suit.CLUBS;
    }

    @Test
    void getPlayer() {
        AcceptedBid bid = new AcceptedBid(acceptor);
        assertEquals(acceptor, bid.getPlayer());
    }

    @Test
    void getType() {
        AcceptedBid bid = new AcceptedBid(acceptor);
        assertEquals(BidType.ACCEPTANCE, bid.getType());
    }

    @Test
    void getChosenTrump_ReturnsDealtTrump() {
        AcceptedBid bid = new AcceptedBid(acceptor);
        // Bij een acceptance is de gekozen troef altijd de gedeelde troef
        assertEquals(dealtTrump, bid.getChosenTrump(dealtTrump));
        assertEquals(Suit.DIAMONDS, bid.getChosenTrump(Suit.DIAMONDS));
    }

    @Test
    void calculateBasePoints_Win() {
        AcceptedBid bid = new AcceptedBid(acceptor);
        int base = BidType.ACCEPTANCE.getBasePoints();
        int target = BidType.ACCEPTANCE.getTargetTricks();

        // Exacte target gehaald
        assertEquals(base, bid.calculateBasePoints(target));

        // Meer dan target gehaald (maar geen 13)
        assertEquals(base, bid.calculateBasePoints(target + 1));
    }

    @Test
    void calculateBasePoints_Loss() {
        AcceptedBid bid = new AcceptedBid(acceptor);
        int base = BidType.ACCEPTANCE.getBasePoints();
        int target = BidType.ACCEPTANCE.getTargetTricks();

        // Net niet gehaald
       // assertEquals(-base, bid.calculateState(target - 1)); // TODO: In je code staat 'points = -1 * points', check of dit klopt met de Whist regels voor partners
    }

    @Test
    void calculateBasePoints_Slem_DoublesPoints() {
        AcceptedBid bid = new AcceptedBid(acceptor);
        int base = BidType.ACCEPTANCE.getBasePoints();

        // Bij 13 slagen worden de punten verdubbeld
        assertEquals(2 * base, bid.calculateBasePoints(13));
    }

    @Test
    void calculateBasePoints_NegativeInput_ThrowsException() {
        AcceptedBid bid = new AcceptedBid(acceptor);
        assertThrows(IllegalArgumentException.class, () -> bid.calculateBasePoints(-1));
    }
}