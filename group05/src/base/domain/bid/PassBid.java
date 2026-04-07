package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.Player;

import java.util.List;

/**
 * Represents a player's decision to pass during the auction.
 * Functions as a fallback bid when a player opts out of the current contract.
 * @param player The player who decided to pass.
 * @author Tommy Wu
 * @since 25/02/26
 */
public record PassBid(Player player) implements Bid {

    public PassBid {
        if (player == null) {throw new IllegalArgumentException("Player cannot be null.");}
    }

    @Override
    public Player getPlayer() {return player;}

    /**
     * Determines the team for this bid.
     * Since a Pass bid never wins the bidding phase, this is purely to satisfy the Bid interface.
     *
     * @param allBids    All bids placed during the round.
     * @param allPlayers All players in the game.
     * @return A list containing only the passing player.
     */
    @Override
    public List<Player> getTeam(List<Bid> allBids, List<Player> allPlayers) {
        return List.of(player);
    }

    @Override
    public BidType getType() {return BidType.PASS;}

    @Override
    public Suit determineTrump(Suit dealtTrump) {
        if (dealtTrump == null) {throw new IllegalArgumentException("Dealt trump suit cannot be null.");}
        return dealtTrump;
    }

    /**
     * Calculates the points for a pass.
     * @param tricksWon The number of tricks won (irrelevant for a pass).
     * @return 0, as passing does not award or lose any points.
     */
    @Override
    public int calculateBasePoints(int tricksWon) {
        if (tricksWon < 0) {throw new IllegalArgumentException("there can't be negative tricks won.");}
        // Passing awards 0 points.
        return BidType.PASS.getBasePoints();
    }
}