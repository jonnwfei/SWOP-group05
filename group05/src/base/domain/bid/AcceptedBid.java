package base.domain.bid;

import base.domain.card.Suit;

/**
 * Represents a contract where a player accepts another player's Proposal.
 * @author Tommy Wu
 * @since 25/02/2026
 */
public record AcceptedBid() implements Bid {


    @Override
    public int teamSize() {return 2;}


    @Override
    public BidType getType() {return BidType.ACCEPTANCE;}

    /**
     * An Accepted Proposal is always played using the originally dealt trump suit.
     *
     * @param dealtTrump The default trump suit dealt at the start.
     * @return The dealt trump suit.
     * @throws IllegalArgumentException if dealtTrump is null.
     */
    @Override
    public Suit determineTrump(Suit dealtTrump) {
        if (dealtTrump == null) {throw new IllegalArgumentException("Dealt trump suit cannot be null.");}
        return dealtTrump;
    }
}
