package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.Player;

import java.util.List;

/**
 * Represents a contract where a player bids to play alone to win all tricks.
 * @param player The player attempting the contract.
 * @param bidType The specific SOLO rank (e.g., SOLO, SOLO_SLIM).
 * @param trump The trump suit for this contract.
 * @author Tommy Wu
 * @since 25/02/2026
 */
public record SoloBid(Player player, BidType bidType, Suit trump) implements Bid {

    public SoloBid {
        if (player == null) {throw new IllegalArgumentException("Player cannot be null.");}
        if (bidType == null) {throw new IllegalArgumentException("BidType cannot be null.");}
        if (bidType.getCategory() != BidCategory.SOLO) {throw new IllegalArgumentException("SoloBid requires a SOLO category!");}
    }

    @Override
    public Player getPlayer() {return player;}

    /**
     * Determines the bidding team. In a Solo contract, the bidder always plays alone.
     *
     * @param allBids    All bids placed during the round.
     * @param allPlayers All players in the game.
     * @return A list containing only the solo bidder.
     */
    @Override
    public List<Player> getTeam(List<Bid> allBids, List<Player> allPlayers) {
        return List.of(player);
    }

    @Override
    public BidType getType() {return bidType;}

    /**
     * Retrieves the trump suit for this Solo round.
     *
     * @param dealtTrump The default trump suit dealt at the start.
     * @return The specific trump suit associated with this Solo bid.
     */
    @Override
    public Suit determineTrump(Suit dealtTrump) {return trump;}


    @Override
    public int calculateBasePoints(int tricksWon) {
        if (tricksWon < 0) {throw new IllegalArgumentException("there can't be negative tricks won.");}
        int points = bidType.getBasePoints();
        if (tricksWon < bidType.getTargetTricks()) {
            points = -1 * points;
            return points;
        }
            return points;
        }
}
