package base.domain.bid;

/**
 * Acts as the centralized registry and Information Expert for all valid contracts (bids) in the game.
 *
 * This enum implements the Flyweight pattern by permanently storing the static rules
 * associated with each bid type, such as the required number of tricks to win,
 * the base point value awarded (or penalized), and the broader category the bid belongs to.
 * @author Tommy Wu
 * @since 26/2/26
 */
public enum BidType {
    /** The default null-bid when a player chooses not to participate. */
    PASS(0, 0, BidCategory.PASS),

    /** A temporary state looking for a partner. Should be resolved before the Bidding Phase ends. */
    PROPOSAL(0, 0, BidCategory.PROPOSAL),

    /** A proposal to play alone, requiring 5 tricks. */
    SOLO_PROPOSAL(5, 6, BidCategory.PROPOSAL),

    /** Forms a team of two players (Proposer and Acceptor), requiring 8 tricks total. */
    ACCEPTANCE(8, 2, BidCategory.ACCEPTANCE),

    /** play alone requiring 9 tricks. Player may choose a new trump suit. */
    ABONDANCE_9(9, 15, BidCategory.ABONDANCE),

    /** play alone requiring 9 tricks using the Original Trump (OT) suit. */
    ABONDANCE_9_OT(9, 15, BidCategory.ABONDANCE),

    /** play alone requiring 10 tricks. Player may choose a new trump suit. */
    ABONDANCE_10(10, 18, BidCategory.ABONDANCE),

    /** play alone requiring 10 tricks using the Original Trump (OT) suit. */
    ABONDANCE_10_OT(10, 18, BidCategory.ABONDANCE),

    /** play alone requiring exactly 0 tricks. No trump suit exists during this round, multiple players can play simultaneously. */
    MISERIE(0, 21, BidCategory.MISERIE),

    /** play alone requiring 11 tricks. Player may choose a new trump suit. */
    ABONDANCE_11(11, 24, BidCategory.ABONDANCE),

    /** play alone requiring 11 tricks using the Original Trump (OT) suit. */
    ABONDANCE_11_OT(11, 24, BidCategory.ABONDANCE),

    /** play alone requiring 12 tricks using the Original Trump (OT) suit. */
    ABONDANCE_12_OT(12, 27, BidCategory.ABONDANCE),

    /** play alone requiring exactly 0 tricks. The player's cards must be revealed after the first trick, multiple players can play simultaneously. */
    OPEN_MISERIE(0, 42, BidCategory.MISERIE),

    /** Solo play requiring all 13 tricks. Player may choose a new trump suit. */
    SOLO(13, 75, BidCategory.SOLO),

    /** The highest bid. Solo play requiring all 13 tricks. */
    SOLO_SLIM(13, 90, BidCategory.SOLO);

    private final int targetTricks;
    private final int basePoints;
    private final BidCategory bidCategory;

    /**
     * Constructs a specific bid type with its immutable, static game rules.
     *
     * @param targetTricks The exact number of tricks required to win the contract (e.g., 0 for Miserie, 9 for Abondance).
     * @param basePoints   The point value awarded on victory, or deducted on defeat.
     * @param bidCategory  The broader classification of the bid used for object validation.
     */
    BidType(int targetTricks, int basePoints, BidCategory bidCategory) {
        this.targetTricks = targetTricks;
        this.basePoints = basePoints;
        this.bidCategory = bidCategory;
    }

    /**
     * Retrieves the target number of tricks required to fulfill this contract.
     * * @return the required trick count.
     */
    public int getTargetTricks() {return targetTricks;}

    /**
     * Retrieves the base point value of this contract for scoring purposes.
     * * @return the base points without accounting for excess tricks won.
     */
    public int getBasePoints() {return basePoints;}

    /**
     * Retrieves the category of this bid, used primarily by Bid records
     * to validate their state during instantiation.
     * * @return the associated {@link BidCategory}.
     */
    public BidCategory getCategory() {return bidCategory;}
}