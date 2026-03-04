package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.trick.Trick;

import java.util.List;

public record abondanceBid(Player player, BidRank bidRank, Suit trump) implements Bid {
    @Override
    public List<Player> getTeam() {
        return List.of(player);
    }

    @Override
    public BidType getType() {
        return BidType;
    }

    @Override
    public Suit getChosenTrump(Suit dealtTrump) {
        return trump;
    }

    @Override
    public boolean checkWin(int tricksWon) {
        return tricksWon >= bidRank.getTargetTricks();
    }

    @Override
    public int calculateBasePoints(int tricksWon) {
        return bidRanbidk.getBasePoints();
    }
}
