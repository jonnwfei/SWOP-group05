package base.domain.bid;

/**
 * @author Tommy Wu
 * @since 26/2/26
 */
public enum BidRank {
    PASS(0),
    PROPOSAL(0),  //should not exist after the bidding phase of round
    SOLO_PROPOSAL(5),
    ACCEPTANCE(8),
    ABONDANCE_9(9),
    ABONDANCE_9_OT(9),
    ABONDANCE_10(10),
    ABONDANCE_10_OT(10),
    MISERIE(0),
    ABONDANCE_11(11),
    ABONDANCE_11_OT(11),
    ABONDANCE_12(12),
    ABONDANCE_12_OT(12),
    OPEN_MISERIE(0),
    SOLO(13),
    SOLO_SLIM(13);

    private final int targetTricks;

    // The constructor that attaches the number to the name
    BidRank(int targetTricks) {
        this.targetTricks = targetTricks;
    }

    public int getTargetTricks() {
        return targetTricks;
    }
}