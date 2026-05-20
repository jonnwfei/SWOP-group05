package base.domain.round;

import base.domain.bid.Bid;
import base.domain.bid.BidCategory;
import base.domain.card.Suit;
import base.domain.player.PlayerId;
import base.domain.scores.ScoringParameters;
import base.domain.scores.ScoringRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An immutable Value Object representing the finalized rules of the round.
 */
public record RoundContract(
        Bid winningBid,
        Suit trumpSuit,
        List<PlayerId> biddingTeam,
        List<PlayerId> defendingTeam,
        int multiplier
) {
    // Compact constructor for defensive programming & immutability
    public RoundContract {
        Objects.requireNonNull(winningBid, "Winning bid cannot be null.");
        Objects.requireNonNull(biddingTeam, "Bidding team cannot be null.");
        Objects.requireNonNull(defendingTeam, "Defending team cannot be null.");

        if (biddingTeam.isEmpty()) {
            throw new IllegalArgumentException("Bidding team cannot be empty.");
        }
        if (defendingTeam.isEmpty()) {
            throw new IllegalArgumentException("Defending team cannot be empty.");
        }
        if (biddingTeam.size() + defendingTeam.size() != 4) {
            throw new IllegalArgumentException("A round contract must involve exactly 4 players.");
        }
        if (multiplier < 1) {
            throw new IllegalArgumentException("Multiplier must be at least 1.");
        }

        // Defensive copying ensures the lists cannot be mutated externally after creation
        biddingTeam = List.copyOf(biddingTeam);
        defendingTeam = List.copyOf(defendingTeam);
    }

    /**
     * Evaluates the outcome using the LATEST rules from the registry.
     */
    public Map<PlayerId, Integer> evaluateOutcome(TrickLedger ledger, ScoringRegistry scoringRegistry) {
        Objects.requireNonNull(ledger, "TrickLedger cannot be null.");
        Objects.requireNonNull(scoringRegistry, "ScoringRegistry cannot be null.");

        Map<PlayerId, Integer> deltas = new HashMap<>();

        // 1. Fetch the dynamic parameters from the registry, NOT the bid
        ScoringParameters params = scoringRegistry.getParameters(this.winningBid.getType());

        // 2. Miserie requires evaluating each player individually
        if (this.winningBid.getType().getCategory() == BidCategory.MISERIE) {
            return evaluateMiserie(ledger, params);
        }

        // 3. Standard calculation using the fetched parameters
        int tricksWon = ledger.getTricksWonByTeam(biddingTeam);

        // 4. Calculate base points and apply multiplier
        int basePoints = params.calculatePoints(tricksWon) * multiplier;

        // 5. Distribute points (Standard zero-sum logic)
        for (PlayerId p : biddingTeam) deltas.put(p, basePoints);

        for (PlayerId p : defendingTeam) {
            int defenderDelta = (biddingTeam.size() == 2) ? -basePoints : (-basePoints / 3);
            deltas.put(p, defenderDelta);
        }

        return deltas;
    }

    /**
     * Evaluates Miserie bids. Because multiple players can bid Miserie simultaneously,
     * each bid is treated as a separate 1v3 contract, and the deltas are summed together.
     */
    private Map<PlayerId, Integer> evaluateMiserie(TrickLedger ledger, ScoringParameters params) {
        Map<PlayerId, Integer> aggregatedDeltas = new HashMap<>();

        // 1. Initialize all players to 0 to make addition easy
        for (PlayerId p : biddingTeam) aggregatedDeltas.put(p, 0);
        for (PlayerId p : defendingTeam) aggregatedDeltas.put(p, 0);

        // 2. Evaluate each Miserie bidder individually (1v3)
        for (PlayerId bidder : biddingTeam) {

            // For Miserie, winning any trick is a failure.
            // We pass 1 to simulate failing the bounds check (0 min, 0 max).
            int tricksWon = ledger.hasPlayerWonAnyTrick(bidder) ? 1 : 0;

            // This perfectly calculates +21 (win) or -21 (loss)
            int bidderPoints = params.calculatePoints(tricksWon) * multiplier;

            // The 3 opponents split the inverse
            int opponentPoints = -bidderPoints / 3;

            // 3. Apply the results of this specific 1v3 game to the aggregated map
            for (PlayerId player : aggregatedDeltas.keySet()) {
                if (player.equals(bidder)) {
                    aggregatedDeltas.put(player, aggregatedDeltas.get(player) + bidderPoints);
                } else {
                    aggregatedDeltas.put(player, aggregatedDeltas.get(player) + opponentPoints);
                }
            }
        }

        return aggregatedDeltas;
    }
}