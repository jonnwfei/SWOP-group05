package base.domain.bid;

/**
 * @author Tommy Wu
 * @since 26/2/26
 */
public enum BidType {
    PASS(0, 0, BidCategory.PASS),
    PROPOSAL(0, 0, BidCategory.PROPOSAL),  //should not exist after the bidding phase of round
    SOLO_PROPOSAL(5, 6, BidCategory.PROPOSAL),
    ACCEPTANCE(8, 2, BidCategory.PROPOSAL),
    ABONDANCE_9(9, 15, BidCategory.ABONDANCE),
    ABONDANCE_9_OT(9, 15, BidCategory.ABONDANCE),
    ABONDANCE_10(10, 18, BidCategory.ABONDANCE),
    ABONDANCE_10_OT(10, 18, BidCategory.ABONDANCE),
    MISERIE(0, 21, BidCategory.MISERIE),
    ABONDANCE_11(11, 24, BidCategory.ABONDANCE),
    ABONDANCE_11_OT(11, 24, BidCategory.ABONDANCE),
    ABONDANCE_12_OT(12, 27, BidCategory.ABONDANCE),
    OPEN_MISERIE(0, 42, BidCategory.MISERIE),
    SOLO(13, 75, BidCategory.SOLO),
    SOLO_SLIM(13, 90, BidCategory.SOLO);

    private final int targetTricks;
    private final int basePoints;
    private final BidCategory bidCategory;

    // The constructor that attaches the number to the name
    BidType(int targetTricks, int basePoints, BidCategory bidCategory) {
        this.targetTricks = targetTricks;
        this.basePoints = basePoints;
        this.bidCategory = bidCategory;
    }

    public int getTargetTricks() {
        return targetTricks;
    }
    public int getBasePoints() {return basePoints;}
    public BidCategory getBidCategory() {return bidCategory;}
}