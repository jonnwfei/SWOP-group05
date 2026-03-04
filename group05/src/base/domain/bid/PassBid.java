package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.trick.Trick;

import java.util.Collections;
import java.util.List;

/**
 * Represents a player choosing not to bid. 
 * Acts as a safe "Null Object" during the Bidding Phase.
 */
public record PassBid(Player player) implements Bid {

    @Override
    public List<Player> getTeam() {
        return Collections.emptyList();
    }

    @Override
    public BidType getType() {
        return BidType.PASS;
    }

    @Override
    public Suit getChosenTrump(Suit dealtTrump) {
        // A PassBid will never win the Bidding Phase, so the Round will never 
        // actually ask it for the trump suit. Returning null is perfectly safe.
        return null;
    }

    @Override
    public boolean checkWin(int teamTricks) {
        // You cannot win a round by passing.
        return false;
    }

    @Override
    public int calculateBasePoints(int wonTricks) {
        // Passing awards 0 points.
        return BidType.PASS.getBasePoints();
    }
}