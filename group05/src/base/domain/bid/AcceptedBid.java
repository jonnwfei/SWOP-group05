package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.trick.Trick;

import java.util.List;

public record AcceptedBid(Player proposer, Player acceptor) implements Bid {
    @Override
    public List<Player> getTeam() {
        return List.of(proposer, acceptor);
    }

    @Override
    public BidType getType() {
        return BidType.ACCEPTANCE;
    }

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
        return base;
    }
}
