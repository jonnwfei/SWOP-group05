package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.player.PlayerId;

import java.util.List;

/**
 * Represents a player's initial offer to form a partnership.
 * Acts as a transitional contract that must be resolved (accepted, passed,
 * or upgraded to a Solo Proposal) before the Playing Phase begins.
 * @author Tommy Wu
 * @since 25/02/2026
 */
public record ProposalBid() implements Bid {

    @Override
    public int teamSize() {return 2;}

    @Override
    public BidType getType() {return BidType.PROPOSAL;}

    /**
     * A Proposal bid is always played using the originally dealt trump suit.
     *
     * @param dealtTrump The default trump suit dealt at the start.
     * @return The originally dealt trump suit.
     * @throws IllegalArgumentException if dealtTrump is null.
     */
    @Override
    public Suit determineTrump(Suit dealtTrump) {
        if (dealtTrump == null) {throw new IllegalArgumentException("Dealt trump suit cannot be null.");}
        return dealtTrump;
    }
}