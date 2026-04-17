package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.PlayerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BidType Enum Rules & Factory")
class BidTypeTest {

    private PlayerId testPlayerId;
    private Suit testSuit;

    @BeforeEach
    void setUp() {
        // Arrange: Using lightweight PlayerId instead of physical Player objects
        testPlayerId = new PlayerId("player-123");
        testSuit = Suit.HEARTS;
    }

    @Nested
    @DisplayName("Static Data & Getters (Flyweight Pattern)")
    class StaticDataTests {

        @Test
        @DisplayName("PASS holds correct static game rules")
        void pass_ReturnsCorrectStaticData() {
            assertEquals(0, BidType.PASS.getTargetTricks());
            assertEquals(0, BidType.PASS.getBasePoints());
            assertEquals(BidCategory.PASS, BidType.PASS.getCategory());
            assertFalse(BidType.PASS.getRequiresSuit());
        }

        @Test
        @DisplayName("MISERIE holds correct static game rules")
        void miserie_ReturnsCorrectStaticData() {
            assertEquals(0, BidType.MISERIE.getTargetTricks());
            assertEquals(21, BidType.MISERIE.getBasePoints());
            assertEquals(BidCategory.MISERIE, BidType.MISERIE.getCategory());
            assertFalse(BidType.MISERIE.getRequiresSuit());
        }

        @Test
        @DisplayName("ABONDANCE_9 holds correct static game rules")
        void abondance9_ReturnsCorrectStaticData() {
            assertEquals(9, BidType.ABONDANCE_9.getTargetTricks());
            assertEquals(15, BidType.ABONDANCE_9.getBasePoints());
            assertEquals(BidCategory.ABONDANCE, BidType.ABONDANCE_9.getCategory());
            assertTrue(BidType.ABONDANCE_9.getRequiresSuit());
        }
    }

    @Nested
    @DisplayName("Factory Method: instantiate()")
    class FactoryMethodTests {

        @Test
        @DisplayName("PASS instantiates a PassBid")
        void instantiate_Pass_ReturnsPassBid() {
            assertInstanceOf(PassBid.class, BidType.PASS.instantiate(testPlayerId, null));
        }

        @Test
        @DisplayName("PROPOSAL instantiates a ProposalBid")
        void instantiate_Proposal_ReturnsProposalBid() {
            assertInstanceOf(ProposalBid.class, BidType.PROPOSAL.instantiate(testPlayerId, null));
        }

        @Test
        @DisplayName("SOLO_PROPOSAL instantiates a SoloProposalBid")
        void instantiate_SoloProposal_ReturnsSoloProposalBid() {
            assertInstanceOf(SoloProposalBid.class, BidType.SOLO_PROPOSAL.instantiate(testPlayerId, null));
        }

        @Test
        @DisplayName("ACCEPTANCE instantiates an AcceptedBid")
        void instantiate_Acceptance_ReturnsAcceptedBid() {
            assertInstanceOf(AcceptedBid.class, BidType.ACCEPTANCE.instantiate(testPlayerId, null));
        }

        @ParameterizedTest(name = "{0} instantiates an AbondanceBid")
        @EnumSource(mode = EnumSource.Mode.INCLUDE, names = {
                "ABONDANCE_9", "ABONDANCE_9_OT",
                "ABONDANCE_10", "ABONDANCE_10_OT",
                "ABONDANCE_11", "ABONDANCE_11_OT",
                "ABONDANCE_12", "ABONDANCE_12_OT"
        })
        void instantiate_AbondanceVariants_ReturnsAbondanceBid(BidType type) {
            assertInstanceOf(AbondanceBid.class, type.instantiate(testPlayerId, testSuit));
        }

        @ParameterizedTest(name = "{0} instantiates a MiserieBid")
        @EnumSource(mode = EnumSource.Mode.INCLUDE, names = {"MISERIE", "OPEN_MISERIE"})
        void instantiate_MiserieVariants_ReturnsMiserieBid(BidType type) {
            assertInstanceOf(MiserieBid.class, type.instantiate(testPlayerId, null));
        }

        @ParameterizedTest(name = "{0} instantiates a SoloBid")
        @EnumSource(mode = EnumSource.Mode.INCLUDE, names = {"SOLO", "SOLO_SLIM"})
        void instantiate_SoloVariants_ReturnsSoloBid(BidType type) {
            assertInstanceOf(SoloBid.class, type.instantiate(testPlayerId, testSuit));
        }

        @ParameterizedTest(name = "{0} instantiates a TroelBid")
        @EnumSource(mode = EnumSource.Mode.INCLUDE, names = {"TROEL", "TROELA"})
        void instantiate_TroelVariants_ReturnsTroelBid(BidType type) {
            // FIX: Because TroelBid now uses PlayerId and Suit (passed in by BidState),
            // it safely instantiates without needing to inspect an actual hand for Aces!
            assertInstanceOf(TroelBid.class, type.instantiate(testPlayerId, testSuit));
        }
    }
}