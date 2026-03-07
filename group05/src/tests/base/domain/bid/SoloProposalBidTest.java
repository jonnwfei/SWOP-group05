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

    @BeforeEach
    void setUp() {
        testPlayer = new Player(new HumanStrategy(), "Soloist");
        dealtTrump = Suit.DIAMONDS;
    }

    @Test
    void getPlayer() {
        SoloProposalBid bid = new SoloProposalBid(testPlayer);
        assertEquals(testPlayer, bid.getPlayer());
    }

    @Test
    void getType() {
        SoloProposalBid bid = new SoloProposalBid(testPlayer);
        assertEquals(BidType.SOLO_PROPOSAL, bid.getType());
    }

    @Test
    void getChosenTrump_ReturnsDealtTrump() {
        SoloProposalBid bid = new SoloProposalBid(testPlayer);
        // Omdat het een onveranderd voorstel is, blijft de troef wat er gedeeld is
        assertEquals(dealtTrump, bid.getChosenTrump(dealtTrump));
    }

    @Test
    void calculateBasePoints_Success() {
        SoloProposalBid bid = new SoloProposalBid(testPlayer);
        int base = BidType.SOLO_PROPOSAL.getBasePoints();
        int target = BidType.SOLO_PROPOSAL.getTargetTricks();

        // Scenario: Target gehaald
        assertEquals(base, bid.calculateBasePoints(target));
        // Scenario: Meer dan target gehaald (geen bonus voor overslagen in de huidige code)
        assertEquals(base, bid.calculateBasePoints(target + 1));
    }

    @Test
    void calculateBasePoints_Failure() {
        SoloProposalBid bid = new SoloProposalBid(testPlayer);
        int base = BidType.SOLO_PROPOSAL.getBasePoints();
        int target = BidType.SOLO_PROPOSAL.getTargetTricks();

        // Scenario: Net niet gehaald
        assertEquals(-base, bid.calculateBasePoints(target - 1));
    }

    @Test
    void calculateBasePoints_Slem_DoublesPoints() {
        SoloProposalBid bid = new SoloProposalBid(testPlayer);
        int base = BidType.SOLO_PROPOSAL.getBasePoints();

        // Scenario: Alle 13 slagen gehaald (Slem/Pit)
        assertEquals(2 * base, bid.calculateBasePoints(13));
    }

    @Test
    void calculateBasePoints_NegativeInput_ThrowsException() {
        SoloProposalBid bid = new SoloProposalBid(testPlayer);
        assertThrows(IllegalArgumentException.class, () -> bid.calculateBasePoints(-1));
    }
}