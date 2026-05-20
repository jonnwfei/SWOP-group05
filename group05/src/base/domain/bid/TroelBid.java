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

    @Override
    public Suit determineTrump(Suit dealtTrump) {
        return this.trumpSuit;
    }
}
