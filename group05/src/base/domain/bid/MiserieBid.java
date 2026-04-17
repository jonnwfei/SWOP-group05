package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.player.PlayerId;

import java.util.List;

/**
 * Represents a Miserie contract where the bidder attempts to win zero tricks.
 * In Miserie, there is no trump suit, and multiple players can play simultaneously.
 *
 * @param playerId  The player who made this bid.
 * @param bidType The specific type of Miserie bid (e.g., MISERIE or OPEN_MISERIE).
 * @author Tommy Wu
 * @since 25/02/26
 */
public record MiserieBid(PlayerId playerId, BidType bidType) implements Bid {

    public MiserieBid {
        if (playerId == null) {throw new IllegalArgumentException("player cannot be null.");}
        if (bidType == null) {throw new IllegalArgumentException("BidType cannot be null.");}
        if (bidType.getCategory() != BidCategory.MISERIE) {throw new IllegalArgumentException("MiserieBid requires a MISERIE category!");}
    }

    @Override
    public PlayerId getPlayerId() {return playerId;}

    /**
     * Determines all players participating in the Miserie XOR Open Miserie contract.
     * @param allBids    All bids placed during the round.
     * @param allPlayers All players in the game.
     * @return A list of all players who made a Miserie bid.
     */
    @Override
    public List<PlayerId> getTeam(List<Bid> allBids, List<Player> allPlayers) {
        // Find every player who made any type of Miserie bid this round
        return allBids.stream()
                .filter(bid -> bid.getType() == bidType)
                .map(Bid::getPlayerId)
                .toList();
    }

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