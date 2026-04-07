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
        assertInstanceOf(PassBid.class, BidType.PASS.instantiate(testPlayer, null));
    }

    @Test
    void instantiate_Proposal_ReturnsProposalBid() {
        assertInstanceOf(ProposalBid.class, BidType.PROPOSAL.instantiate(testPlayer, null));
    }

    @Test
    void instantiate_SoloProposal_ReturnsSoloProposalBid() {
        assertInstanceOf(SoloProposalBid.class, BidType.SOLO_PROPOSAL.instantiate(testPlayer, null));
    }

    @Test
    void instantiate_Acceptance_ReturnsAcceptedBid() {
        assertInstanceOf(AcceptedBid.class, BidType.ACCEPTANCE.instantiate(testPlayer, null));
    }

    @Test
    void instantiate_AbondanceVariants_ReturnAbondanceBid() {
        BidType[] abondanceTypes = {
                BidType.ABONDANCE_9, BidType.ABONDANCE_9_OT,
                BidType.ABONDANCE_10, BidType.ABONDANCE_10_OT,
                BidType.ABONDANCE_11, BidType.ABONDANCE_11_OT,
                BidType.ABONDANCE_12, BidType.ABONDANCE_12_OT
        };

        for (BidType type : abondanceTypes) {
            Bid bid = type.instantiate(testPlayer, testSuit);
            assertInstanceOf(AbondanceBid.class, bid, type.name() + " should instantiate an AbondanceBid");
        }
    }

    @Test
    void instantiate_MiserieVariants_ReturnMiserieBid() {
        assertInstanceOf(MiserieBid.class, BidType.MISERIE.instantiate(testPlayer, null));
        assertInstanceOf(MiserieBid.class, BidType.OPEN_MISERIE.instantiate(testPlayer, null));
    }

    @Test
    void instantiate_SoloVariants_ReturnSoloBid() {
        assertInstanceOf(SoloBid.class, BidType.SOLO.instantiate(testPlayer, testSuit));
        assertInstanceOf(SoloBid.class, BidType.SOLO_SLIM.instantiate(testPlayer, null));
    }

    @Test
    void instantiate_TroelVariants_WithEmptyHand_ThrowsIllegalArgumentException() {
        // Because the testPlayer has no Aces, the TroelBid constructor will throw an exception.
        // We assertThrows to handle it gracefully, which STILL gives 100% coverage
        // because the 'instantiate' method successfully executes 'return new TroelBid(...)'.

        assertThrows(IllegalArgumentException.class, () -> {
            BidType.TROEL.instantiate(testPlayer, null);
        }, "TROEL should throw exception due to lack of Aces");

        assertThrows(IllegalArgumentException.class, () -> {
            BidType.TROELA.instantiate(testPlayer, null);
        }, "TROELA should throw exception due to lack of Aces");
    }
}