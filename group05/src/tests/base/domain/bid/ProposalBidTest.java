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
    void constructor_NullPlayer_ThrowsException() {
        // Enforce GRASP invariant: Cannot instantiate a bid without a valid player
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new ProposalBid(null)
        );
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
    void getChosenTrump_NullDealtTrump_ThrowsException() {
        // Enforce GRASP invariant: A Proposal bid relies on the dealt trump, which cannot be null.
        Player testPlayer = new Player(new HumanStrategy(), "Proposer");
        ProposalBid bid = new ProposalBid(testPlayer);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                bid.getChosenTrump(null)
        );

        assertTrue(exception.getMessage().toLowerCase().contains("null"),
                "Exception message should mention null.");
    }

    @Test
    void calculateBasePoints_ExactTarget_ReturnsBasePoints() {
        // Target is 8 tricks. Extra = 0.
        // Expected: 2 base points + 0 extra = 2 points.
        assertEquals(2, bid.calculateBasePoints(8));
    }

    @Test
    void calculateBasePoints_WithExcessTricks_AddsExtraPoints() {
        // 10 tricks won. Target is 8. Extra = 2.
        // Expected: 2 base points + 2 extra = 4 points.
        assertEquals(4, bid.calculateBasePoints(10));
    }

    @Test
    void calculateBasePoints_AllTricks_DoublesTotalPoints() {
        // 13 tricks won. Target is 8. Extra = 5.
        // Expected: (2 base points + 5 extra) * 2 = 14 points.
        assertEquals(14, bid.calculateBasePoints(13));
    }

    @Test
    void calculateBasePoints_FailedBid_ReturnsNegativeBase() {
        // 7 tricks won. Target is 8.
        // Expected: -2 points (losing a standard proposal loses the base points).
        assertEquals(-2, bid.calculateBasePoints(7));
    }

    @Test
    void calculateBasePoints_OutOfBounds_ThrowsException() {
        // Defensive checks for impossible trick counts
        assertThrows(IllegalArgumentException.class, () -> bid.calculateBasePoints(-1));
        assertThrows(IllegalArgumentException.class, () -> bid.calculateBasePoints(14));
    }

    @Test
    void testRecordAccessor() {
        assertEquals(proposer, bid.proposer());
    }
}