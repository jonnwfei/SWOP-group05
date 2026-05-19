package base.domain.bid;

import base.domain.card.Suit;

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
}