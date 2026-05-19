package base.domain.bid;

import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.player.PlayerId;

import java.util.List;

/**
 * Represents a Miserie contract where the bidder attempts to win zero tricks.
 * In Miserie, there is no trump suit, and multiple players can play simultaneously.
 *
 * @param bidType The specific type of Miserie bid (e.g., MISERIE or OPEN_MISERIE).
 * @author Tommy Wu
 * @since 25/02/26
 */
public record MiserieBid(BidType bidType) implements Bid {

    public MiserieBid {
        if (bidType == null) {throw new IllegalArgumentException("BidType cannot be null.");}
        if (bidType.getCategory() != BidCategory.MISERIE) {throw new IllegalArgumentException("MiserieBid requires a MISERIE category!");}
    }
    @Override
    public int teamSize() {return 1;}

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
}