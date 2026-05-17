package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.player.PlayerId;

import java.util.List;

/**
 * Represents a player's decision to pass during the auction.
 * Functions as a fallback bid when a player opts out of the current contract.
 * @author Tommy Wu
 * @since 25/02/26
 */
public record PassBid() implements Bid {

    @Override
    public int teamSize() {return 1;}

    @Override
    public BidType getType() {return BidType.PASS;}

    @Override
    public Suit determineTrump(Suit dealtTrump) {
        if (dealtTrump == null) {throw new IllegalArgumentException("Dealt trump suit cannot be null.");}
        return dealtTrump;
    }

    /**
     * Calculates the points for a pass.
     * @param tricksWon The number of tricks won (irrelevant for a pass).
     * @return 0, as passing does not award or lose any points.
     */
    @Override
    public int calculateBasePoints(int tricksWon) {
        if (tricksWon < 0) {throw new IllegalArgumentException("there can't be negative tricks won.");}
        // Passing awards 0 points.
        return BidType.PASS.getBasePoints();
    }
}