import domain.card.Suit;

import java.util.List;

public record SoloProposalBid(Player player) implements Bid {
    @Override
    public List<Player> getTeam() {
        return List.of(player);
    }

    @Override
    public BidRank getRank() {
        return BidRank.SOLO_PROPOSAL;
    }

    @Override
    public Suit getChosenTrump(Suit dealtTrump) {
        return dealtTrump;
    }

    @Override
    public boolean checkWin(List<Trick> tricksWon) {
        if (tricksWon.isEmpty()) {return false;}
        return tricksWon.size() >= 5 && tricksWon.getLast().hasTrump();
    }

    @Override
    public int calculateBasePoints(int tricksWon) {
        int base = 6;
        int extra = tricksWon - 5;
        if (extra > 0) {base = base + 3*extra;}
        return base;
    }
}
