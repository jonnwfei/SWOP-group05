package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.trick.Trick;

import java.util.List;

public record AcceptedBid(Player acceptor) implements Bid {
    @Override
    public Player getPlayer() {return acceptor;}

    @Override
    public BidType getType() {return BidType.ACCEPTANCE;}

    @Override
    public Suit getChosenTrump(Suit dealtTrump) {
        return dealtTrump;
    }

    @Override
    public boolean checkWin(int tricksWon) {
        return tricksWon >= BidType.ACCEPTANCE.getTargetTricks();
    }

    @Override
    public int calculateBasePoints(int tricksWon) {
        int base = BidType.ACCEPTANCE.getBasePoints();
        int extra = tricksWon - BidType.ACCEPTANCE.getTargetTricks();
        if (extra > 0) {base = base + 3*extra;}
        if (tricksWon == 13) {base = 2*base;}
        return base;
    }
}
