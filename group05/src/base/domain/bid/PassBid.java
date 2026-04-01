package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.trick.Trick;

import java.util.Collections;
import java.util.List;

/**
 * Represents a player's decision to pass during the auction.
 * Functions as a fallback bid when a player opts out of the current contract.
 */
public record PassBid(Player player) implements Bid {

    public PassBid {
        if (player == null) {throw new IllegalArgumentException("Proposer cannot be null.");}

    }

    @Override
    public Player getPlayer() {return player;}

    @Override
    public BidType getType() {return BidType.PASS;}

    @Override
    public Suit getChosenTrump(Suit dealtTrump) {
        // A PassBid will never win the Bidding Phase, so the Round will never 
        // actually ask it for the trump suit. Returning null is perfectly safe.
        return null;
    }

    @Override
    public int calculateBasePoints(int tricksWon) {
        if (tricksWon < 0) {throw new IllegalArgumentException("there can't be negative tricks won.");}
        // Passing awards 0 points.
        return BidType.PASS.getBasePoints();
    }
}