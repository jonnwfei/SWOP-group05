package base.domain.round;

import base.domain.bid.Bid;
import base.domain.bid.BidCategory;
import base.domain.player.PlayerId;
import base.domain.scores.ScoringParameters;
import base.domain.scores.ScoringRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoundContract {
    private final Bid winningBid;
    private final List<PlayerId> biddingTeam;
    private final List<PlayerId> defendingTeam;
    private final int multiplier;

    public RoundContract(Bid winningBid, List<PlayerId> biddingTeam, List<PlayerId> defendingTeam, int multiplier) {
        this.winningBid = winningBid;
        this.multiplier = multiplier;
        this.biddingTeam = List.copyOf(biddingTeam);
        this.defendingTeam = List.copyOf(defendingTeam);
    }

    /**
     * Evaluates the outcome using the LATEST rules from the registry.
     */
    public Map<PlayerId, Integer> evaluateOutcome(int tricksWon, List<PlayerId> miserieWinners, ScoringRegistry scoringRegistry) {
        Map<PlayerId, Integer> deltas = new HashMap<>();

        // 1. Fetch the dynamic parameters from the registry, NOT the bid
        ScoringParameters params = scoringRegistry.getParameters(this.winningBid.getType());

        // 2. Miserie requires evaluating each player individually
        if (this.winningBid.getType().getCategory() == BidCategory.MISERIE) {
            return evaluateMiserie(miserieWinners, params);
        }

        // 3. Standard calculation using the fetched parameters
        int basePoints = params.calculatePoints(tricksWon);

        // 4. Distribute points (Standard zero-sum logic)
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
    private Map<PlayerId, Integer> evaluateMiserie(List<PlayerId> miserieWinners, ScoringParameters params) {
        Map<PlayerId, Integer> aggregatedDeltas = new HashMap<>();

        // 1. Initialize all players to 0 to make addition easy
        for (PlayerId p : biddingTeam) aggregatedDeltas.put(p, 0);
        for (PlayerId p : defendingTeam) aggregatedDeltas.put(p, 0);

        // 2. Evaluate each Miserie bidder individually (1v3)
        for (PlayerId bidder : biddingTeam) {

            // For Miserie, winning any trick is a failure.
            // A player is in miserieWinners if they won ZERO tricks.
            boolean success = miserieWinners.contains(bidder);

            // We pass 1 to simulate failing the bounds check (0 min, 0 max) if success is false.
            int tricksWonMetric = success ? 0 : 1;

            // This perfectly calculates +21 (win) or -21 (loss)
            int bidderPoints = params.calculatePoints(tricksWonMetric) * multiplier;

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

    public Bid getWinningBid() {
        return winningBid;
    }

    public List<PlayerId> getBiddingTeam() {
        return biddingTeam;
    }
}
