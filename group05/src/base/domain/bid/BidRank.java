package base.domain.bid;

/**
 * @author Tommy Wu
 * @since 26/2/26
 */
public enum BidRank {
    PASS,
    PROPOSAL, // at the end of the bidding phase of round, should have converted to : PASS, SOLO_PROPOSAL or ACCEPTANCE
    SOLO_PROPOSAL,
    ACCEPTANCE,
    ABONDANCE_9, //player can choose Trump suit of choice
    ABONDANCE_9_OT, //OT = Original Trump
    ABONDANCE_10,
    ABONDANCE_10_OT,
    MISERIE,
    ABONDANCE_11,
    ABONDANCE_11_OT,
    ABONDANCE_12,
    ABONDANCE_12_OT,
    OPEN_MISERIE,
    SOLO,
    SOLO_SLIM
}