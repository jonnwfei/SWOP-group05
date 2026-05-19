package base.domain.round;

import base.domain.bid.BidType;
import base.domain.player.Player;
import base.domain.scores.ScoringParameters;
import base.domain.scores.ScoringRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoundContract {
    private final BidType winningBid;
    private final List<Player> biddingTeam;
    private final List<Player> defendingTeam;
    private final int multiplier;

    public RoundContract(BidType winningBid, List<Player> biddingTeam, List<Player> defendingTeam, int multiplier) {
        this.winningBid = winningBid;
        this.multiplier = multiplier;

        this.biddingTeam = biddingTeam;
        this.defendingTeam = defendingTeam;
    }

    /**
     * Evaluates the outcome using the LATEST rules from the registry.
     * By passing the registry here, Use Case 4.9 works flawlessly!
     */
    public Map<Player, Integer> evaluateOutcome(TrickLedger ledger, ScoringRegistry registry) {
        // 1. Fetch the rules dynamically at the exact moment of evaluation
        ScoringParameters parameters = registry.getParameters(this.winningBid);

        Map<Player, Integer> deltas = new HashMap<>();

        // 2. Do the math using the fresh parameters
        int tricksWon = ledger.getTricksWonByTeam(biddingTeam);
        int basePoints = parameters.calculatePoints(tricksWon) * multiplier;

        // 3. Distribute points (Standard zero-sum logic)
        for (Player p : biddingTeam) deltas.put(p, basePoints);

        for (Player p : defendingTeam) {
            int defenderDelta = (biddingTeam.size() == 2) ? -basePoints : (-basePoints / 3);
            deltas.put(p, defenderDelta);
        }

        return deltas;
    }
}