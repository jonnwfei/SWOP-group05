package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.Player;

import java.util.List;

public record SoloBid(Player player, BidType bidType, Suit trump) implements Bid {
    public SoloBid {
        if (bidType.getBidCategory() != BidCategory.SOLO) {
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
    public boolean checkWin(int tricksWon) {return tricksWon >= bidType.getTargetTricks();}

    @Override
    public int calculateBasePoints(int tricksWon) {return getType().getBasePoints();}
}
