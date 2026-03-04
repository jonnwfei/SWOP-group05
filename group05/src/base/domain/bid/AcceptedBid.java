import domain.card.Suit;

import java.util.List;

public record AcceptedBid(Player proposer, Player acceptor) implements Bid {
    @Override
    public List<Player> getTeam() {
        return List.of(proposer, acceptor);
    }

    @Override
    public BidRank getRank() {
        return BidRank.ACCEPTANCE;
    }

    @Override
    public Suit getChosenTrump(Suit dealtTrump) {
        return dealtTrump;
    }

    @Override
    public boolean checkWin(List<Trick> tricksWon) {
        if (tricksWon.isEmpty()) {return false;}
        return tricksWon.size() >= 8 && tricksWon.getLast().hasTrump();
    }

    @Override
    public int calculateBasePoints(int tricksWon) {
        int base = 2;
        int extra = tricksWon - 8;
        if (extra > 0) {
            base = base + extra;
        }
        return base;
    }
}
