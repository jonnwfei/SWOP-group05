package base.domain.scores;

import base.domain.bid.BidType;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static base.domain.round.TrickLedger.MAX_TRICKS;

/**
 * Acts as the centralized, thread-safe Configuration Registry for all scoring math.
 */
public class ScoringRegistry {

    // ConcurrentHashMap ensures thread-safe reads/updates during runtime
    private final Map<BidType, ScoringParameters> scoringSettings = new ConcurrentHashMap<>();

    public ScoringRegistry() {
        // --- PROPOSAL / ACCEPTANCE ---
        scoringSettings.put(BidType.PROPOSAL, new ScoringParameters(6, MAX_TRICKS, 6, 3, true));
        scoringSettings.put(BidType.ACCEPTANCE, new ScoringParameters(6, MAX_TRICKS, 2, 1, true));

        // --- TROEL(A) ---
        scoringSettings.put(BidType.TROEL, new ScoringParameters(8, MAX_TRICKS, 4, 2, true));
        scoringSettings.put(BidType.TROELA, new ScoringParameters(8, MAX_TRICKS, 4, 2, true));

        // --- ABONDANCE ---
        scoringSettings.put(BidType.ABONDANCE_9, new ScoringParameters(9, MAX_TRICKS, 15, 0, false));
        scoringSettings.put(BidType.ABONDANCE_10, new ScoringParameters(10, MAX_TRICKS, 18, 0, false));
        scoringSettings.put(BidType.ABONDANCE_11, new ScoringParameters(11, MAX_TRICKS, 24, 0, false));
        scoringSettings.put(BidType.ABONDANCE_12_OT, new ScoringParameters(12, MAX_TRICKS, 27, 0, false));

        // --- SOLO ---
        scoringSettings.put(BidType.SOLO, new ScoringParameters(13, MAX_TRICKS, 75, 0, false));
        scoringSettings.put(BidType.SOLO_SLIM, new ScoringParameters(13, MAX_TRICKS, 90, 0, false));

        // --- MISERIE ---
        scoringSettings.put(BidType.MISERIE, new ScoringParameters(0, 0, 21, 0, false));
        scoringSettings.put(BidType.OPEN_MISERIE, new ScoringParameters(0, 0, 42, 0, false));
    }

    /**
     * Retrieves the scoring rules for a specific bid.
     *
     * @param bidType The type of bid to look up.
     * @return The associated ScoringParameters.
     * @throws NullPointerException if bidType is null.
     * @throws IllegalArgumentException if the bidType is PASS or not found in the registry.
     */
    public ScoringParameters getParameters(BidType bidType) {
        validateBidTypeIsScorable(bidType);

        ScoringParameters params = scoringSettings.get(bidType);
        if (params == null) {
            throw new IllegalArgumentException("Critical configuration error: No scoring parameters found for BidType: " + bidType);
        }

        return params;
    }

    /**
     * Updates the scoring rules for a specific bid dynamically (Supports UC 4.9).
     *
     * @param bidType The type of bid to update.
     * @param newParameters The new mathematical rules for the bid.
     * @throws NullPointerException if either argument is null.
     * @throws IllegalArgumentException if the bidType is PASS.
     */
    public void updateParameters(BidType bidType, ScoringParameters newParameters) {
        validateBidTypeIsScorable(bidType);
        Objects.requireNonNull(newParameters, "New ScoringParameters cannot be null.");

        scoringSettings.put(bidType, newParameters);
    }

    /**
     * Returns a read-only snapshot of all current settings.
     *
     * @return An unmodifiable map of the current scoring configurations.
     */
    public Map<BidType, ScoringParameters> getAllParameters() {
        return Map.copyOf(scoringSettings);
    }

    /**
     * Defensive guard to prevent assigning or requesting points for PASS bids.
     */
    private void validateBidTypeIsScorable(BidType bidType) {
        Objects.requireNonNull(bidType, "BidType cannot be null.");
        if (bidType == BidType.PASS) {
            throw new IllegalArgumentException("BidType.PASS does not have scoring parameters.");
        }
    }
}