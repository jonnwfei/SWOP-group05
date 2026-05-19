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

    /**
     * Calculates the points won or lost based on tricks taken.
     * Earns extra points (+3) for each overtrick, and doubles the total score if all 13 tricks are won.
     *
     * @param tricksWon The number of tricks won by the solo player.
     * @return Positive calculated points if the contract was met, negative base points if failed.
     * @throws IllegalArgumentException if tricksWon is negative.
     */
    @Override
    public int calculateBasePoints(int tricksWon) {
        if (tricksWon < 0) {throw new IllegalArgumentException("there can't be negative tricks won.");}
        int points = BidType.SOLO_PROPOSAL.getBasePoints();
        int extra = tricksWon - BidType.SOLO_PROPOSAL.getTargetTricks();
        if (extra < 0) {
            points = -1 * points;
            return points;
        }
        points += 3*extra;
        if (tricksWon == 13) {points = 2*points;}
        return points;
    }
}
