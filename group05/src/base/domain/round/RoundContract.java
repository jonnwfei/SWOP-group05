package base.domain.round;

import base.domain.bid.Bid;
import base.domain.card.Suit;
import base.domain.player.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoundContract {
    private final Bid winningBid;
    private final Suit trumpSuit;
    private final List<Player> biddingTeam;
    private final List<Player> defendingTeam;
    private final int multiplier;

    public RoundContract(Bid winningBid, Suit trumpSuit, List<Player> biddingTeam, List<Player> defendingTeam, int multiplier) {
        this.winningBid = winningBid;
        this.multiplier = multiplier;
        this.trumpSuit = trumpSuit;

        this.biddingTeam = biddingTeam;
        this.defendingTeam = defendingTeam;
    }

    /**
     * Evaluates the outcome using the LATEST rules from the registry.
     */
    public Map<Player, Integer> evaluateOutcome(TrickLedger ledger) {
        // 1. Fetch the rules dynamically at the exact moment of evaluation

        Map<Player, Integer> deltas = new HashMap<>();

        // 2. Do the math using the fresh parameters
        int tricksWon = ledger.getTricksWonByTeam(biddingTeam);
        int basePoints = this.winningBid.calculateBasePoints(tricksWon);

        // 3. Distribute points (Standard zero-sum logic)
        for (Player p : biddingTeam) deltas.put(p, basePoints);

        for (Player p : defendingTeam) {
            int defenderDelta = (biddingTeam.size() == 2) ? -basePoints : (-basePoints / 3);
            deltas.put(p, defenderDelta);
        }

        return deltas;
    }
}