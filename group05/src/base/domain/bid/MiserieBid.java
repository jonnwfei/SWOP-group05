package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.Player;

import java.util.List;

public record MiserieBid(Player player, BidType bidType) implements Bid {

    public MiserieBid {
        if (player == null) {throw new IllegalArgumentException("Proposer cannot be null.");}
        if (bidType.getCategory() != BidCategory.MISERIE) {throw new IllegalArgumentException("MiserieBid requires a MISERIE category!");}
    }

    @Override
    public Player getPlayer() {return player;}

    @Override
    public BidType getType() {return bidType;}

    @Override
    public Suit getChosenTrump(Suit dealtTrump) {
        return null;
    }


    @Override
    public int calculateBasePoints(int tricksWon) {
        if (tricksWon < 0) {throw new IllegalArgumentException("there can't be negative tricks won.");}
        int points = bidType.getBasePoints();
        if (tricksWon > bidType.getTargetTricks()) {
            points = -1 * points;
            return points;
        }
        return points;
    }
}