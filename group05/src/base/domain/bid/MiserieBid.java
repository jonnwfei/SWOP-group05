package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.Player;

import java.util.List;

public record MiserieBid(Player player, BidType bidType) implements Bid {

    public MiserieBid {
        if (player == null) {throw new IllegalArgumentException("player cannot be null.");}
        if (bidType == null) {throw new IllegalArgumentException("BidType cannot be null.");}
        if (bidType.getCategory() != BidCategory.MISERIE) {throw new IllegalArgumentException("MiserieBid requires a MISERIE category!");}
    }

    @Override
    public Player getPlayer() {return player;}

    @Override
    public List<Player> getTeam(List<Bid> allBids, List<Player> allPlayers) {
        int totalCards = allPlayers.stream().mapToInt(p -> p.getHand().size()).sum();
        if (totalCards != 52) {
            throw new IllegalStateException("getTeam() can only be called before the play phase begins!");
        }
        // Find every player who made any type of Miserie bid this round
        return allBids.stream()
                .filter(bid -> bid.getType() == bidType)
                .map(Bid::getPlayer)
                .toList();    }

    @Override
    public BidType getType() {return bidType;}

    @Override
    public Suit getChosenTrump(Suit dealtTrump) {
        return null;
    }


    @Override
    public int calculateBasePoints(int tricksWon) {
        if (tricksWon < 0) {throw new IllegalArgumentException("there can't be negative tricks won.");}
        int points = bidType.getBasePoints();
        if (tricksWon > bidType.getTargetTricks()) {
            points = -1 * points;
            return points;
        }
        return points;
    }
}