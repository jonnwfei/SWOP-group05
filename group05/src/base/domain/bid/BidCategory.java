package base.domain.bid;

/**
 * Defines the broad categories of Whist bids.
 * Used to group specific BidTypes for rule checks (e.g., finding a partner vs playing alone).
 */
public enum BidCategory {
    PASS,       // No bid
    PROPOSAL,   // Asking for a partner
    ACCEPTANCE, // Partnering with a Proposer
    SOLO,       // Playing alone with dealt trump
    ABONDANCE,  // Playing alone, choosing trump (9+ tricks)
    TROEL,      // Forced bids, played with partner when having 3 or more aces
    MISERIE     // Playing alone to win exactly 0 tricks
}