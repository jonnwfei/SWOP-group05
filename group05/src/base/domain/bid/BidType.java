package base.domain.bid;

import base.domain.card.Suit;

/**
 * Acts as the centralized identifier and factory for all valid contracts (bids) in the game.
 * This enum implements the Flyweight pattern by storing the static categorical rules
 * associated with each bid type.
 * <p>
 * Note: Scoring parameters (target tricks, base points) are explicitly excluded from this enum
 * and are managed by the ScoringRegistry to support dynamic/retroactive scoring changes.
 *
 * @author Tommy Wu
 * @since 26/02/26
 */
public enum BidType {
    /** The default null-bid when a player chooses not to participate. */
    PASS(BidCategory.PASS, false) {
        @Override
        public Bid instantiate(Suit trumpSuit) { return new PassBid(); }
    },

    /** A temporary state looking for a partner. Should be resolved before the Bidding Phase ends. */
    PROPOSAL(BidCategory.PROPOSAL, false) {
        @Override
        public Bid instantiate(Suit trumpSuit) { return new ProposalBid(); }
    },

    /** A proposal to play alone. */
    SOLO_PROPOSAL(BidCategory.PROPOSAL, false) {
        @Override
        public Bid instantiate(Suit trumpSuit) { return new SoloProposalBid(); }
    },

    /** Forms a team of two players (Proposer and Acceptor). */
    ACCEPTANCE(BidCategory.ACCEPTANCE, false) {
        @Override
        public Bid instantiate(Suit trumpSuit) { return new AcceptedBid(); }
    },

    /** Play alone. Player may choose a new trump suit. */
    ABONDANCE_9(BidCategory.ABONDANCE, true) {
        @Override
        public Bid instantiate(Suit trumpSuit) { return new AbondanceBid(this, trumpSuit); }
    },

    /** Play alone using the Original Trump (OT) suit. */
    ABONDANCE_9_OT(BidCategory.ABONDANCE, false) {
        @Override
        public Bid instantiate(Suit trumpSuit) { return new AbondanceBid(this, trumpSuit); }
    },

    /** Play alone. Player may choose a new trump suit. */
    ABONDANCE_10(BidCategory.ABONDANCE, true) {
        @Override
        public Bid instantiate(Suit trumpSuit) { return new AbondanceBid(this, trumpSuit); }
    },

    /** Play alone using the Original Trump (OT) suit. */
    ABONDANCE_10_OT(BidCategory.ABONDANCE, false) {
        @Override
        public Bid instantiate(Suit trumpSuit) { return new AbondanceBid(this, trumpSuit); }
    },

    /** Play alone. No trump suit exists during this round, multiple players can play simultaneously. */
    MISERIE(BidCategory.MISERIE, false) {
        @Override
        public Bid instantiate(Suit trumpSuit) { return new MiserieBid(this); }
    },

    /** Play alone. Player may choose a new trump suit. */
    ABONDANCE_11(BidCategory.ABONDANCE, true) {
        @Override
        public Bid instantiate(Suit trumpSuit) { return new AbondanceBid(this, trumpSuit); }
    },

    /** Play alone using the Original Trump (OT) suit. */
    ABONDANCE_11_OT(BidCategory.ABONDANCE, false) {
        @Override
        public Bid instantiate(Suit trumpSuit) { return new AbondanceBid(this, trumpSuit); }
    },

    /** Play alone. Player may choose a new trump suit. */
    ABONDANCE_12(BidCategory.ABONDANCE, true) {
        @Override
        public Bid instantiate(Suit trumpSuit) { return new AbondanceBid(this, trumpSuit); }
    },

    /** Play alone using the Original Trump (OT) suit. */
    ABONDANCE_12_OT(BidCategory.ABONDANCE, false) {
        @Override
        public Bid instantiate(Suit trumpSuit) { return new AbondanceBid(this, trumpSuit); }
    },

    /**
     * A forced team bid triggered when a player holds exactly 3 Aces.
     * The partner is the player holding the 4th missing Ace.
     */
    TROEL(BidCategory.TROEL, false) {
        @Override
        public Bid instantiate(Suit trumpSuit) { return new TroelBid(this, trumpSuit); }
    },

    /**
     * A forced team bid triggered when a player holds all 4 Aces.
     * The partner is the player holding the highest Heart.
     */
    TROELA(BidCategory.TROEL, false) {
        @Override
        public Bid instantiate(Suit trumpSuit) { return new TroelBid(this, trumpSuit); }
    },

    /** Play alone. The player's cards must be revealed after the first trick, multiple players can play simultaneously. */
    OPEN_MISERIE(BidCategory.MISERIE, false) {
        @Override
        public Bid instantiate(Suit trumpSuit) { return new MiserieBid(this); }
    },

    /** Solo play requiring all tricks. Player may choose a new trump suit. */
    SOLO(BidCategory.SOLO, true) {
        @Override
        public Bid instantiate(Suit trumpSuit) { return new SoloBid(this, trumpSuit); }
    },

    /** The highest bid. Solo play requiring all tricks, original trump. */
    SOLO_SLIM(BidCategory.SOLO, false) {
        @Override
        public Bid instantiate(Suit trumpSuit) { return new SoloBid(this, trumpSuit); }
    };

    private final BidCategory bidCategory;
    private final boolean requiresSuit;

    /**
     * Constructs a specific bid type with its immutable categorical rules.
     *
     * @param bidCategory  The broader classification of the bid used for object validation and grouping.
     * @param requiresSuit The boolean flag indicating if the player must actively choose a new trump suit.
     */
    BidType(BidCategory bidCategory, boolean requiresSuit) {
        this.bidCategory = bidCategory;
        this.requiresSuit = requiresSuit;
    }

    public BidCategory getCategory() { return bidCategory; }
    public boolean getRequiresSuit() { return requiresSuit; }

    /**
     * Polymorphically instantiates the correct Bid implementation for this specific BidType.
     * @param trumpSuit The suit chosen by the player (if requiresSuit is true).
     * @return A fully instantiated, immutable Bid object.
     */
    public abstract Bid instantiate(Suit trumpSuit);
}