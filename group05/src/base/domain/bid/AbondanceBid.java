package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.player.PlayerId;

import java.util.List;

/**
 * Contract for Abondance bids (9 to 12 tricks) with a custom trump suit if applicable.
 * @param playerId The proposer of the bid.
 * @param bidType Specific ABONDANCE level (e.g., ABONDANCE_9).
 * @param trump The user-defined trump suit for the round.
 * @author Tommy Wu
 * @since 25/02/2026
 */
public record AbondanceBid(PlayerId playerId, BidType bidType, Suit trump) implements Bid {

    public AbondanceBid {
        if (playerId == null) {throw new IllegalArgumentException("Player cannot be null.");}
        if (bidType == null) {throw new IllegalArgumentException("BidType cannot be null.");}
        if (bidType.getCategory() != BidCategory.ABONDANCE) {throw new IllegalArgumentException("AbondanceBid requires an ABONDANCE rank!");}
    }

    @Override
    public PlayerId getPlayerId() {return playerId;}

    /**
     * Determines the bidding team. In an Abondance contract, the bidder always plays alone.
     *
     * @param allBids    All bids placed during the round.
     * @param allPlayers All players in the game.
     * @return A list containing only the solo bidder.
     */
    @Override
    public List<PlayerId> getTeam(List<Bid> allBids, List<Player> allPlayers) {
        return List.of(playerId);
    }

    @Override
    public BidType getType() {return bidType;}

    /**
     * Retrieves the trump suit for this round.
     * @param dealtTrump The default trump suit dealt at the start.
     * @return The trump suit chosen by the Abondance bidder or the original trump suit.
     */
    @Override
    public Suit determineTrump(Suit dealtTrump) {
        if (dealtTrump == null) {throw new IllegalArgumentException("Dealt trump suit cannot be null.");}
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
