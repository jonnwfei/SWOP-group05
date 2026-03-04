package base.domain.bid;

/**
 * @author Tommy Wu
 * @since 26/2/26
 */
public enum BidType {
    PASS(0, 0),
    PROPOSAL(0, 0),  //should not exist after the bidding phase of round
    SOLO_PROPOSAL(5, 6),
    ACCEPTANCE(8, 2),
    ABONDANCE_9(9, 15),
    ABONDANCE_9_OT(9, 15),
    ABONDANCE_10(10, 18),
    ABONDANCE_10_OT(10, 18),
    MISERIE(0, 21),
    ABONDANCE_11(11, 24),
    ABONDANCE_11_OT(11, 24),
    ABONDANCE_12(12, 27),
    ABONDANCE_12_OT(12, 27),
    OPEN_MISERIE(0, 42),
    SOLO(13, 75),
    SOLO_SLIM(13, 90);

    private final int targetTricks;
    private final int basePoints;

    // The constructor that attaches the number to the name
    BidType(int targetTricks, int basePoints) {
        this.targetTricks = targetTricks;
        this.basePoints = basePoints;
    }

    public int getTargetTricks() {
        return targetTricks;
    }
    public int getBasePoints() {return basePoints;}
}