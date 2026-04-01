package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.trick.Trick;

import java.util.List;

/**
 * Represents a contract where a player accepts another player's Proposal.
 * @param acceptor The player who agreed to the proposal.
 * @author Tommy
 * @since 25/02/2026
 */
public record AcceptedBid(Player acceptor) implements Bid {

    public AcceptedBid {
        if (acceptor == null) {throw new IllegalArgumentException("Proposer cannot be null.");}
    }

    @Override
    public Player getPlayer() {return acceptor;}

    @Override
    public BidType getType() {return BidType.ACCEPTANCE;}

    @Override
    public Suit getChosenTrump(Suit dealtTrump) {
        if (dealtTrump == null) {throw new IllegalArgumentException("Dealt trump suit cannot be null.");}
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
        points += extra;
        if (tricksWon == 13) {points = 2*points;}
        return points;
    }
}
