package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.Player;

import java.util.List;

/**
 * Contract for Abondance bids (9 to 12 tricks) with a custom trump suit if applicable.
 * @param player The proposer of the bid.
 * @param bidType Specific ABONDANCE level (e.g., ABONDANCE_9).
 * @param trump The user-defined trump suit for the round.
 * @author Tommy
 * @since 25/02/2026
 */
public record AbondanceBid(Player player, BidType bidType, Suit trump) implements Bid {

    /**
     * Compact Constructor: Enforces that the {@code bidType} belongs
     * to the ABONDANCE category.
     */
    public AbondanceBid {
        if (player == null) {throw new IllegalArgumentException("Proposer cannot be null.");}
        if (bidType == null) {throw new IllegalArgumentException("BidType cannot be null.");}
        if (bidType.getCategory() != BidCategory.ABONDANCE) {throw new IllegalArgumentException("AbondanceBid requires an ABONDANCE rank!");}
    }

    @Override
    public Player getPlayer() {return player;}

    @Override
    public List<Player> getTeam(List<Bid> allBids, List<Player> allPlayers) {
        int totalCards = allPlayers.stream().mapToInt(p -> p.getHand().size()).sum();
        if (totalCards != 52) {
            throw new IllegalStateException("getTeam() can only be called before the play phase begins!");
        }
        return List.of(player);
    }

    @Override
    public BidType getType() {return bidType;}

    @Override
    public Suit getChosenTrump(Suit dealtTrump) {
        return trump;
    }

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
