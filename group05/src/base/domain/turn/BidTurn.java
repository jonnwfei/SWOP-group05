package base.domain.turn;

import base.domain.bid.BidType;
import base.domain.player.PlayerId;

public record BidTurn(PlayerId player, BidType bidType) {
    /**
     * Instantiates a new BidTurn.
     *
     * @param player     of this bid turn
     * @param bidType    chosen by the player
     */
    public BidTurn {
        if (player == null) throw new IllegalArgumentException("Bid turn: Player cannot be null");
        if (bidType == null) throw new IllegalArgumentException("Bid turn: bidType cannot be null");
    }
}
