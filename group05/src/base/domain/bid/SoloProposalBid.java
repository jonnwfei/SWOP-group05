package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.trick.Trick;

import java.util.List;

public record SoloProposalBid(Player player) implements Bid {
    @Override
    public Player getPlayer() {return player;}

    @Override
    public BidType getType() {return BidType.SOLO_PROPOSAL;}

    @Override
    public Suit getChosenTrump(Suit dealtTrump) {return dealtTrump;}

    @Override
    public boolean checkWin(int tricksWon) {
        return tricksWon >= BidType.SOLO_PROPOSAL.getTargetTricks();
    }

    @Override
    public int calculateBasePoints(int tricksWon) {
        int base = BidType.SOLO_PROPOSAL.getBasePoints();
        int extra = tricksWon - BidType.SOLO_PROPOSAL.getTargetTricks();
        if (extra > 0) {base = base + 3*extra;}
        if (tricksWon == 13) {base = 2*base;}
        return base;
    }
}
