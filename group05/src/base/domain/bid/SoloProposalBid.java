package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.player.PlayerId;

import java.util.List;

/**
 * Contract for a proposer who chooses to play alone after their initial proposal is rejected.
 * The trump suit defaults to the originally dealt trump.
 *
 * @author Tommy Wu
 * @since 25/02/2026
 */
public record SoloProposalBid() implements Bid {

    @Override
    public int teamSize() {return 1;}

    @Override
    public BidType getType() {return BidType.SOLO_PROPOSAL;}

    /**
     * A Solo Proposal is always played using the originally dealt trump suit.
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
