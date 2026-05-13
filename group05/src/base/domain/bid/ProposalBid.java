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

    /**
     * Calculates the points won or lost based on tricks taken.
     * Earns extra points for overtricks, and doubles the score if all 13 tricks are won.
     *
     * @param tricksWon The combined number of tricks won by the team.
     * @return Positive calculated points if the contract was met, negative base points if failed.
     */
    @Override
    public int calculateBasePoints(int tricksWon) {
        if (tricksWon < 0 || tricksWon > 13) {throw new IllegalArgumentException("tricks won is out of bound, min 0 max 13");}
        int points = BidType.ACCEPTANCE.getBasePoints();

        int extra = tricksWon - BidType.ACCEPTANCE.getTargetTricks();
        if (extra < 0) {
            points = -1 * points;
            return points;
        }
        points += extra;
        if (tricksWon == 13) {points = 2*points;}
        return points;
    }
}