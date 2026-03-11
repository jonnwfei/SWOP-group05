package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.trick.Trick;

import java.util.List;

/**
 * Represents a player's initial offer to form a partnership.
 * Acts as a transitional contract that must be resolved (accepted, passed,
 * or upgraded to a Solo Proposal) before the Playing Phase begins.
 */
public record ProposalBid(Player proposer) implements Bid {

    @Override
    public Player getPlayer() {return proposer;}

    @Override
    public BidType getType() {return BidType.PROPOSAL;}

    @Override
    public Suit getChosenTrump(Suit dealtTrump) {
        return dealtTrump;
    }

    @Override
    public int calculateBasePoints(int tricksWon) {
        if (tricksWon < 0) {throw new IllegalArgumentException("there can't be negative tricks won.");}
        int points = BidType.ACCEPTANCE.getBasePoints();

        int extra = tricksWon - BidType.ACCEPTANCE.getTargetTricks();
        if (extra < 0) {
            points = -1 * points;
            return points;
        }

        if (tricksWon == 13) {points = 2*points;}
        return points;
    }
}