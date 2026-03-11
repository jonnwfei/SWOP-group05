package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.HumanStrategy;
import base.domain.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProposalBidTest {

    private Player proposer;
    private Suit dealtTrump;
    private ProposalBid bid;

    @BeforeEach
    void setUp() {
        proposer = new Player(new HumanStrategy(), "Voorsteller");
        dealtTrump = Suit.HEARTS;
        bid = new ProposalBid(proposer);
    }

    @Test
    void getPlayer() {
        assertEquals(proposer, bid.getPlayer());
    }

    @Test
    void getType() {
        assertEquals(BidType.PROPOSAL, bid.getType());
    }

    @Test
    void getChosenTrump_ReturnsDealtTrump() {
        // Volgens de nieuwe implementatie wordt de originele troef gewoon teruggegeven
        assertEquals(dealtTrump, bid.getChosenTrump(dealtTrump));
    }

    @Test
    void calculateBasePoints_ThrowsOnNegativeTricks() {
        // Negatieve tricks moeten de custom IllegalArgumentException gooien
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                bid.calculateBasePoints(-1)
        );
        assertTrue(exception.getMessage().contains("negative tricks won"));
    }

    @Test
    void calculateBasePoints_Loss() {
        int target = BidType.ACCEPTANCE.getTargetTricks();
        int expectedPoints = -1 * BidType.ACCEPTANCE.getBasePoints();

        // 1 trick te weinig
        assertEquals(expectedPoints, bid.calculateBasePoints(target - 1));
        // Slechts 0 tricks gehaald
        assertEquals(expectedPoints, bid.calculateBasePoints(0));
    }

    @Test
    void calculateBasePoints_WinNormal() {
        int target = BidType.ACCEPTANCE.getTargetTricks();
        int expectedPoints = BidType.ACCEPTANCE.getBasePoints();

        // Exact het target gehaald
        assertEquals(expectedPoints, bid.calculateBasePoints(target));
        // Extra slagen gehaald (maar minder dan 13)
        assertEquals(expectedPoints, bid.calculateBasePoints(target + 1));
        assertEquals(expectedPoints, bid.calculateBasePoints(12));
    }

    @Test
    void calculateBasePoints_WinAll() {
        int expectedPoints = 2 * BidType.ACCEPTANCE.getBasePoints();

        // Als alle 13 slagen worden gehaald, worden de punten verdubbeld
        assertEquals(expectedPoints, bid.calculateBasePoints(13));
    }

    @Test
    void testRecordAccessor() {
        assertEquals(proposer, bid.proposer());
    }
}