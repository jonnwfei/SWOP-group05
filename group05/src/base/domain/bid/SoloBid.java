package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.Player;

import java.util.List;

public record SoloBid(Player player, BidType bidType, Suit trump) implements Bid {
    public SoloBid {
        if (bidType.getCategory() != BidCategory.SOLO) {
            throw new IllegalArgumentException("SoloBid requires a SOLO category!");
        }
    }

    @Override
    public Player getPlayer() {return player;}

    @Override
    public BidType getType() {return bidType;}

    @Override
    public Suit getChosenTrump(Suit dealtTrump) {return trump;}


    @Override
    public int calculateBasePoints(int tricksWon) {
        if (tricksWon < 0) {throw new IllegalArgumentException("there can't be negative tricks won.");}
        int points = bidType.getBasePoints();
        if (tricksWon < bidType.getTargetTricks()) {
            points = -1 * points;
            return points;
        }
            return points;
        }
}
