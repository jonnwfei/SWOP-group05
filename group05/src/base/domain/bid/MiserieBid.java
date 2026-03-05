package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.Player;

import java.util.List;

// CHANGE 1: It now takes a List<Player> instead of a single Player!
public record MiserieBid(Player player, BidType bidType) implements Bid {

    public MiserieBid {
        if (bidType.getBidCategory() != BidCategory.MISERIE) {
            throw new IllegalArgumentException("MiserieBid requires a MISERIE category!");
        }
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
    public boolean checkWin(int tricksWon) {
        return tricksWon == 0;
    }

    @Override
    public int calculateBasePoints(int tricksWon) {
        return bidType.getBasePoints();
    }
}