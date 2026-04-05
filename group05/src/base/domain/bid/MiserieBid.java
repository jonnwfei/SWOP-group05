package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.Player;

import java.util.List;

/**
 * Represents a Miserie contract where the bidder attempts to win zero tricks.
 * In Miserie, there is no trump suit, and multiple players can play simultaneously.
 *
 * @param player  The player who made this bid.
 * @param bidType The specific type of Miserie bid (e.g., MISERIE or OPEN_MISERIE).
 * @author Tommy Wu
 * @since 25/02/26
 */
public record MiserieBid(Player player, BidType bidType) implements Bid {

    public MiserieBid {
        if (player == null) {throw new IllegalArgumentException("player cannot be null.");}
        if (bidType == null) {throw new IllegalArgumentException("BidType cannot be null.");}
        if (bidType.getCategory() != BidCategory.MISERIE) {throw new IllegalArgumentException("MiserieBid requires a MISERIE category!");}
    }

    @Override
    public Player getPlayer() {return player;}

    /**
     * Determines all players participating in the Miserie contract.
     * @param allBids    All bids placed during the round.
     * @param allPlayers All players in the game.
     * @return A list of all players who made a Miserie bid.
     * @throws IllegalStateException if called after cards have been played.
     */
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

    /**
     * Miserie is played without a trump suit.
     * * @param dealtTrump The default trump suit dealt at the start.
     * @return null, as Miserie has no trump.
     */
    @Override
    public Suit determineTrump(Suit dealtTrump) {
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