package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.Player;

/**
 * Acts as the centralized registry and Information Expert for all valid contracts (bids) in the game.
 * This enum implements the Flyweight pattern by permanently storing the static rules
 * associated with each bid type, such as the required number of tricks to win,
 * the base point value awarded (or penalized), and the broader category the bid belongs to.
 * @author Tommy Wu
 * @since 26/02/26
 */
public enum BidType {
    /** The default null-bid when a player chooses not to participate. */
    PASS(0, 0, BidCategory.PASS, false) {
        @Override
        public Bid instantiate(Player player, Suit chosenSuit) {
            return new PassBid((player));
        }
    },

    /** A temporary state looking for a partner. Should be resolved before the Bidding Phase ends. */
    PROPOSAL(0, 0, BidCategory.PROPOSAL, false) {
        @Override
        public Bid instantiate(Player player, Suit TrumpSuit) {
            return new ProposalBid(player);
        }
    },

    /** A proposal to play alone, requiring 5 tricks. */
    SOLO_PROPOSAL(5, 6, BidCategory.PROPOSAL, false) {
        @Override
        public Bid instantiate(Player player, Suit TrumpSuit) {
            return new SoloProposalBid(player);

        }
    },

    /** Forms a team of two players (Proposer and Acceptor), requiring 8 tricks total. */
    ACCEPTANCE(8, 2, BidCategory.ACCEPTANCE, false) {
        @Override
        public Bid instantiate(Player player, Suit TrumpSuit) {
            return new AcceptedBid(player);
        }
    },

    /** play alone requiring 9 tricks. Player may choose a new trump suit. */
    ABONDANCE_9(9, 15, BidCategory.ABONDANCE, true) {
        @Override
        public Bid instantiate(Player player, Suit chosenSuit) {
            return new AbondanceBid(player, this, chosenSuit);
        }
    },

    /** play alone requiring 9 tricks using the Original Trump (OT) suit. */
    ABONDANCE_9_OT(9, 15, BidCategory.ABONDANCE, false) {
        @Override
        public Bid instantiate(Player player, Suit TrumpSuit) {
            return new AbondanceBid(player, this, TrumpSuit);
        }
    },

    /** play alone requiring 10 tricks. Player may choose a new trump suit. */
    ABONDANCE_10(10, 18, BidCategory.ABONDANCE, true) {
        @Override
        public Bid instantiate(Player player, Suit TrumpSuit) {
            return new AbondanceBid(player, this, TrumpSuit);
        }
    },

    /** play alone requiring 10 tricks using the Original Trump (OT) suit. */
    ABONDANCE_10_OT(10, 18, BidCategory.ABONDANCE, false) {
        @Override
        public Bid instantiate(Player player, Suit TrumpSuit) {
            return new AbondanceBid(player, this, TrumpSuit);
        }
    },

    /** play alone requiring exactly 0 tricks. No trump suit exists during this round, multiple players can play simultaneously. */
    MISERIE(0, 21, BidCategory.MISERIE, false) {
        @Override
        public Bid instantiate(Player player, Suit TrumpSuit) {
            return new MiserieBid(player, this);

        }
    },

    /** play alone requiring 11 tricks. Player may choose a new trump suit. */
    ABONDANCE_11(11, 24, BidCategory.ABONDANCE, true) {
        @Override
        public Bid instantiate(Player player, Suit TrumpSuit) {
            return new AbondanceBid(player, this, TrumpSuit);
        }
    },

    /** play alone requiring 11 tricks using the Original Trump (OT) suit. */
    ABONDANCE_11_OT(11, 24, BidCategory.ABONDANCE, false) {
        @Override
        public Bid instantiate(Player player, Suit TrumpSuit) {
            return new AbondanceBid(player, this, TrumpSuit);
        }
    },

    /** play alone requiring 12 tricks. Player may choose a new trump suit. */
    ABONDANCE_12(12, 27, BidCategory.ABONDANCE, true) {
        @Override
        public Bid instantiate(Player player, Suit TrumpSuit) {
            return new AbondanceBid(player, this, TrumpSuit);
        }
    },

    /** play alone requiring 12 tricks using the Original Trump (OT) suit. */
    ABONDANCE_12_OT(12, 27, BidCategory.ABONDANCE, false) {
        @Override
        public Bid instantiate(Player player, Suit TrumpSuit) {
            return new AbondanceBid(player, this, TrumpSuit);
        }
    },

    /** * A forced team bid triggered when a player holds exactly 3 Aces.
     * The partner is the player holding the 4th missing Ace, playing together to win at least 8 tricks.
     */
    TROEL(8, 4,BidCategory.TROEL , false) {
        @Override
        public Bid instantiate(Player player, Suit TrumpSuit) {
            return new TroelBid(player, this);
        }
    },

    /** * A forced team bid triggered when a player holds all 4 Aces.
     * The partner is the player holding the highest Heart, playing together to win at least 9 tricks.
     */
    TROELA(9, 4, BidCategory.TROEL, false) {
        @Override
        public Bid instantiate(Player player, Suit TrumpSuit) {
            return new TroelBid(player, this);
        }
    },

    /** play alone requiring exactly 0 tricks. The player's cards must be revealed after the first trick, multiple players can play simultaneously. */
    OPEN_MISERIE(0, 42, BidCategory.MISERIE, false) {
        @Override
        public Bid instantiate(Player player, Suit TrumpSuit) {
            return new MiserieBid(player, this);

        }
    },

    /** Solo play requiring all 13 tricks. Player may choose a new trump suit. */
    SOLO(13, 75, BidCategory.SOLO, true) {
        @Override
        public Bid instantiate(Player player, Suit TrumpSuit) {
            return new SoloBid(player, this, TrumpSuit);

        }
    },

    /** The highest bid. Solo play requiring all 13 tricks. */
    SOLO_SLIM(13, 90, BidCategory.SOLO, false) {
        @Override
        public Bid instantiate(Player player, Suit TrumpSuit) {
            return new SoloBid(player, this, TrumpSuit);

        }
    };

    private final int targetTricks;
    private final int basePoints;
    private final BidCategory bidCategory;
    private  final boolean requiresSuit;

    /**
     * Constructs a specific bid type with its immutable, static game rules.
     *
     * @param targetTricks The exact number of tricks required to win the contract (e.g., 0 for Miserie, 9 for Abondance).
     * @param basePoints   The point value awarded on victory, or deducted on defeat.
     * @param bidCategory  The broader classification of the bid used for object validation.
     * @param requiresSuit The boolean flag indicating if the player must actively choose a new trump suit
     */
    BidType(int targetTricks, int basePoints, BidCategory bidCategory, boolean requiresSuit) {
        this.targetTricks = targetTricks;
        this.basePoints = basePoints;
        this.bidCategory = bidCategory;
        this.requiresSuit = requiresSuit;
    }

    //GETTERS
    public int getTargetTricks() {return targetTricks;}
    public int getBasePoints() {return basePoints;}
    public BidCategory getCategory() {return bidCategory;}
    public boolean getRequiresSuit() {return requiresSuit;}

    /**
     * Polymorphically instantiates the correct Bid implementation for this specific BidType.
     * @param player The player making the bid.
     * @param trumpSuit The suit chosen by the player (if requiresSuit is true).
     * @return A fully instantiated, immutable Bid object.
     */
    public abstract Bid instantiate(Player player, Suit trumpSuit);}