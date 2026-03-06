package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.Player;

import java.util.List;

public record AbondanceBid(Player player, BidType bidType, Suit trump) implements Bid {

    // PERFECT GRASP: The Compact Constructor ensures the Class Invariant.
    // It runs automatically right before the object is created.
    public AbondanceBid {
        if (bidType.getCategory() != BidCategory.ABONDANCE) {
            throw new IllegalArgumentException("AbondanceBid requires an ABONDANCE rank!");
        }
    }

    @Override
    public Player getPlayer() {return player;}

    @Override
    public BidType getType() {return bidType;}

    @Override
    public Suit getChosenTrump(Suit dealtTrump) {
        return trump;
    }

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
