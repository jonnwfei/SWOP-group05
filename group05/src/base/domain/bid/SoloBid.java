package base.domain.bid;

import base.domain.card.Suit;

/**
 * Represents a contract where a player bids to play alone to win all tricks.
 * @param bidType The specific SOLO rank (e.g., SOLO, SOLO_SLIM).
 * @param trump The trump suit for this contract.
 * @author Tommy Wu
 * @since 25/02/2026
 */
public record SoloBid(BidType bidType, Suit trump) implements Bid {

    public SoloBid {
        if (bidType == null) {throw new IllegalArgumentException("BidType cannot be null.");}
        if (bidType.getCategory() != BidCategory.SOLO) {throw new IllegalArgumentException("SoloBid requires a SOLO category!");}
    }

    @Override
    public int teamSize() {return 1;}

    @Override
    public BidType getType() {return bidType;}

    /**
     * Retrieves the trump suit for this Solo round.
     *
     * @param dealtTrump The default trump suit dealt at the start.
     * @return The specific trump suit associated with this Solo bid.
     */
    @Override
    public Suit determineTrump(Suit dealtTrump) {
        if (dealtTrump == null) {throw new IllegalArgumentException("Dealt trump suit cannot be null.");}
        return trump;
    }
}
