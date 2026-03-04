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
    public BidRank getRank() {
        // should check wether abondance?
        return bidRank;
    }

    @Override
    public Suit getChosenTrump(Suit dealtTrump) {
        return trump;
    }

    @Override
    public boolean checkWin(List<Trick> tricksWon) {
        // should check which abondance? or change BidRank to hold amount of tricks needed?
        return false;
    }

    @Override
    public int calculateBasePoints(int tricksWon) {
        // same question
        return 0;
    }
}
