package base.domain.bid;

import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.player.PlayerId;

import java.util.Comparator;
import java.util.List;


/**
 * Represents a forced contract triggered when a player holds 3 or 4 Aces.
 * The player forms a team with a forced partner (the holder of the 4th Ace, or the highest Heart).
 *
 * @param bidType   The specific Troel rank (TROEL or TROELA).
 * @param trumpSuit The suit of the missing 4th Ace (for TROEL). Ignored for TROELA.
 * @author Tommy Wu
 * @since 01/04/2026
 */
public record TroelBid(BidType bidType, Suit trumpSuit) implements Bid {

    public TroelBid(BidType bidType, Suit trumpSuit) {
        if (bidType == null) throw new IllegalArgumentException("BidType can't be null");
        if (bidType.getCategory() != BidCategory.TROEL) throw new IllegalArgumentException("TroelBid requires a TROEL category!");

        this.bidType = bidType;

        // Rule Enforcement: If it's TROELA, the trump is ALWAYS Hearts.
        if (bidType == BidType.TROELA) {
            this.trumpSuit = Suit.HEARTS;
        } else {
            // For standard TROEL, the caller MUST provide the suit of the missing Ace.
            if (trumpSuit == null) throw new IllegalArgumentException("TROEL requires the suit of the missing Ace.");
            this.trumpSuit = trumpSuit;
        }
    }
    @Override
    public int teamSize() {return 2;}

    @Override
    public BidType getType() {return bidType;}

    /**
     * Calculates the points won or lost based on tricks taken.
     * Earns extra points (+2) for each overtrick, and doubles the total score if all 13 tricks are won.
     *
     * @param tricksWon The combined number of tricks won by the team.
     * @return Positive calculated points if the contract was met, negative base points if failed.
     */
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
    public Suit determineTrump(Suit dealtTrump) {
        return this.trumpSuit;
    }
}
