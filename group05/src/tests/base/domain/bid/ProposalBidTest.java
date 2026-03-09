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

    @BeforeEach
    void setUp() {
        proposer = new Player(new HumanStrategy(), "Voorsteller");
        dealtTrump = Suit.HEARTS;
    }

    @Test
    void getPlayer() {
        ProposalBid bid = new ProposalBid(proposer);
        assertEquals(proposer, bid.getPlayer());
    }

    @Test
    void getType() {
        ProposalBid bid = new ProposalBid(proposer);
        assertEquals(BidType.PROPOSAL, bid.getType());
    }

    @Test
    void getChosenTrump_ThrowsIllegalStateException() {
        ProposalBid bid = new ProposalBid(proposer);
        // Volgens je Javadoc mag deze methode NOOIT worden aangeroepen in de speelfase
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                bid.getChosenTrump(dealtTrump)
        );
        assertTrue(exception.getMessage().contains("CRITICAL"));
    }

    @Test
    void calculateBasePoints_ThrowsIllegalStateException() {
        ProposalBid bid = new ProposalBid(proposer);
        // Een onopgelost voorstel heeft geen scorelogica
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                bid.calculateBasePoints(8)
        );
        assertTrue(exception.getMessage().contains("CRITICAL"));
    }

    @Test
    void testRecordAccessor() {
        ProposalBid bid = new ProposalBid(proposer);
        assertEquals(proposer, bid.proposer());
    }
}