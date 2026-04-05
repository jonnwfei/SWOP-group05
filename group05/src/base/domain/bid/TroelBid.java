package base.domain.bid;

import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.player.Player;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public record TroelBid(Player player, BidType bidType) implements Bid {

    public TroelBid {
        if (player == null) {throw new IllegalArgumentException("player can't be null");}
        if (bidType == null) {throw new IllegalArgumentException("bidType can't be null");}
        if (bidType.getCategory() != BidCategory.TROEL) {throw new IllegalArgumentException("TroelBid requires a TROEL category!");}

        long aceCount = player.getHand().stream()
                .filter(card -> card.rank() == Rank.ACE)
                .count();

        if (aceCount < 3) {throw new IllegalArgumentException("Player doesn't meet conditions");}
        if (aceCount == 4 && bidType == BidType.TROEL) {throw new IllegalArgumentException("wrong bidType given");}
        if (aceCount == 3 && bidType == BidType.TROELA) {throw new IllegalArgumentException("wrong bidType given");}
    }

    @Override
    public Player getPlayer() {return player;}

    @Override
    public List<Player> getTeam(List<Bid> allBids, List<Player> allPlayers) {
        Player partner;
        switch (bidType) {
            //find player with the missing ace card
            case TROEL -> {
                Card missingAce = new Card(this.getChosenTrump(null), Rank.ACE);
                partner = allPlayers.stream()
                          .filter(p -> p.hasCard(missingAce))
                          .findFirst()
                          .orElse(null);
            }

            //find player with the highest card in the suit of hearts
            case TROELA -> partner = allPlayers.stream()
                                     .filter( p -> !p.equals(player))
                                     .max(Comparator.comparing(p -> p.getHighestRankOfSuit(Suit.HEARTS), Comparator.nullsFirst(Comparator.naturalOrder())))
                                     .orElse(null);

            default -> throw new IllegalArgumentException("wrong bidType given");
        }

        if (partner == null) {throw new IllegalStateException("Partner not found. This method must be called before cards are played from hands.");}

        return List.of(player, partner);
    }

    @Override
    public BidType getType() {return bidType;}

    @Override
    public int calculateBasePoints(int tricksWon) {
        if (tricksWon < 0) {throw new IllegalArgumentException("there can't be negative tricks won.");}
        int points = bidType.getBasePoints();
        int extra = tricksWon - bidType.getTargetTricks();
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
