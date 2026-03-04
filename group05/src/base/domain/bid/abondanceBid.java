package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.trick.Trick;

import java.util.List;

public record abondanceBid(Player player, BidType bidType, Suit trump) implements Bid {

    // PERFECT GRASP: The Compact Constructor ensures the Class Invariant.
    // It runs automatically right before the object is created.
    public abondanceBid {
        if (bidType.getBidCategory() != BidCategory.ABONDANCE) {
            throw new IllegalArgumentException("AbondanceBid requires an ABONDANCE rank!");
        }
    }

    @Override
    public List<Player> getTeam() {
        return List.of(player);
    }

    @Override
    public BidType getType() {
        return bidType;
    }

    @Override
    public Suit getChosenTrump(Suit dealtTrump) {
        return trump;
    }

    @Override
    public boolean checkWin(int tricksWon) {
        return tricksWon >= bidType.getTargetTricks();
    }

    @Override
    public int calculateBasePoints(int tricksWon) {
        return bidType.getBasePoints();
    }
}
