package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.trick.Trick;

import java.util.List;

/**
 * Contract for a proposer who chooses to play alone after their initial proposal is rejected.
 * The trump suit defaults to the originally dealt trump.
 *
 * @param player The original proposer now playing solo.
 */
public record SoloProposalBid(Player player) implements Bid {
    @Override
    public Player getPlayer() {return player;}

    @Override
    public BidType getType() {return BidType.SOLO_PROPOSAL;}

    @Override
    public Suit getChosenTrump(Suit dealtTrump) {return dealtTrump;}

    @Override
    public int calculateBasePoints(int tricksWon) {
        if (tricksWon < 0) {throw new IllegalArgumentException("there can't be negative tricks won.");}
        int points = BidType.SOLO_PROPOSAL.getBasePoints();
        int extra = tricksWon - BidType.SOLO_PROPOSAL.getTargetTricks();
        if (extra < 0) {
            points = -1 * points;
            return points;
        }
        if (tricksWon == 13) {points = 2*points;}
        return points;
    }
}
