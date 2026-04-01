package base.domain.bid;

import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.player.Player;

import java.util.Arrays;
import java.util.List;

public record TroelBid(Player player, BidType bidType) implements Bid {

    public TroelBid {
        if (player == null) {
            throw new IllegalArgumentException("player can't be null");
        }
        if (bidType.getCategory() != BidCategory.TROEL) {
            throw new IllegalArgumentException("TroelBid requires a TROEL category!");
        }

        long aceCount = player.getHand().stream()
                .filter(card -> card.rank() == Rank.ACE)
                .count();

        if (aceCount < 3) {
            throw new IllegalArgumentException("Player doesn't meet conditions");
        }
        if (aceCount == 4 && bidType == BidType.TROEL) {
            throw new IllegalArgumentException("wrong bidType given");
        }
        if (aceCount == 3 && bidType == BidType.TROELA) {
            throw new IllegalArgumentException("wrong bidType given");
        }
    }

    @Override
    public Player getPlayer() {return player;}

    @Override
    public BidType getType() {return bidType;}

    @Override
    public int calculateBasePoints(int tricksWon) {
        if (tricksWon < 0) {throw new IllegalArgumentException("there can't be negative tricks won.");}
        int points = BidType.TROEL.getBasePoints();
        int extra = tricksWon - BidType.TROEL.getTargetTricks();
        if (extra < 0) {
            points = -1 * points;
            return points;
        }
        points += 2*extra;
        if (tricksWon == 13) {points = 2*points;}
        return points;
    }


    @Override
    public Suit getChosenTrump(Suit dealtTrump) {
        List<Suit> aces = getAces();
        if (aces.size() == 4) {
            return Suit.HEARTS;
        }
        return Arrays.stream(Suit.values())
                .filter(suit -> !aces.contains(suit))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing 4th ace not found"));    }

    private List<Suit> getAces() {
       return player.getHand().stream().filter(card -> card.rank() == Rank.ACE).map(Card::suit).toList();
    }

}
