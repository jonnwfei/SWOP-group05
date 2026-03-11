package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.HumanStrategy;
import base.domain.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BidTypeTest {

    private Player testPlayer;
    private Suit testSuit;

    @BeforeEach
    void setUp() {
        testPlayer = new Player(new HumanStrategy(), "Tester");
        testSuit = Suit.HEARTS;
    }

    // -------- GETTER & FLYWEIGHT TESTS --------

    @Test
    void enumGetters_ReturnCorrectStaticData() {
        // Test a few representative enums to ensure the constructor mapped fields correctly

        // PASS check
        assertEquals(0, BidType.PASS.getTargetTricks());
        assertEquals(0, BidType.PASS.getBasePoints());
        assertEquals(BidCategory.PASS, BidType.PASS.getCategory());
        assertFalse(BidType.PASS.getRequiresSuit());

        // MISERIE check
        assertEquals(0, BidType.MISERIE.getTargetTricks());
        assertEquals(21, BidType.MISERIE.getBasePoints());
        assertEquals(BidCategory.MISERIE, BidType.MISERIE.getCategory());
        assertFalse(BidType.MISERIE.getRequiresSuit());

        // ABONDANCE_9 check
        assertEquals(9, BidType.ABONDANCE_9.getTargetTricks());
        assertEquals(15, BidType.ABONDANCE_9.getBasePoints());
        assertEquals(BidCategory.ABONDANCE, BidType.ABONDANCE_9.getCategory());
        assertTrue(BidType.ABONDANCE_9.getRequiresSuit());
    }

    // -------- FACTORY METHOD (INSTANTIATE) TESTS --------

    @Test
    void instantiate_Pass_ReturnsPassBid() {
        Bid bid = BidType.PASS.instantiate(testPlayer, null);
        assertInstanceOf(PassBid.class, bid, "PASS should instantiate a PassBid");
        assertEquals(testPlayer, bid.getPlayer());
    }

    @Test
    void instantiate_Proposal_ReturnsProposalBid() {
        Bid bid = BidType.PROPOSAL.instantiate(testPlayer, null);
        assertInstanceOf(ProposalBid.class, bid, "PROPOSAL should instantiate a ProposalBid");
    }

    @Test
    void instantiate_SoloProposal_ReturnsSoloProposalBid() {
        Bid bid = BidType.SOLO_PROPOSAL.instantiate(testPlayer, null);
        assertInstanceOf(SoloProposalBid.class, bid, "SOLO_PROPOSAL should instantiate a SoloProposalBid");
    }

    @Test
    void instantiate_Acceptance_ReturnsAcceptedBid() {
        Bid bid = BidType.ACCEPTANCE.instantiate(testPlayer, null);
        assertInstanceOf(AcceptedBid.class, bid, "ACCEPTANCE should instantiate an AcceptedBid");
    }

    @Test
    void instantiate_Abondance_ReturnsAbondanceBid() {
        Bid bid = BidType.ABONDANCE_9.instantiate(testPlayer, testSuit);
        assertInstanceOf(AbondanceBid.class, bid, "ABONDANCE_9 should instantiate an AbondanceBid");

        // Ensure the factory passed the suit correctly
        assertEquals(testSuit, bid.getChosenTrump(Suit.SPADES));
    }

    @Test
    void instantiate_Miserie_ReturnsMiserieBid() {
        Bid bid = BidType.MISERIE.instantiate(testPlayer, null);
        assertInstanceOf(MiserieBid.class, bid, "MISERIE should instantiate a MiserieBid");
    }

    @Test
    void instantiate_Solo_ReturnsSoloBid() {
        Bid bid = BidType.SOLO.instantiate(testPlayer, testSuit);
        assertInstanceOf(SoloBid.class, bid, "SOLO should instantiate a SoloBid");

        // Ensure the factory passed the suit correctly
        assertEquals(testSuit, bid.getChosenTrump(Suit.SPADES));
    }

    @Test
    void instantiate_AllTypes_DoNotCrash() {
        // Ultimate safeguard: loop through EVERY enum constant and ensure calling instantiate()
        // doesn't throw unexpected NullPointerExceptions or other crashes.
        for (BidType type : BidType.values()) {
            assertDoesNotThrow(() -> {
                Bid bid = type.instantiate(testPlayer, testSuit);
                assertNotNull(bid, type.name() + " instantiated a null Bid!");
                assertEquals(type, bid.getType(), type.name() + " returned a bid with mismatched type!");
            });
        }
    }
}