package base.domain.turn;

import base.domain.bid.BidType;
import base.domain.player.PlayerId;

public record BidTurn(PlayerId playerId, BidType bidType) {
    /**
     * Instantiates a new BidTurn.
     *
     * @param playerId     of this bid turn
     * @param bidType    chosen by the player
     */
    public BidTurn {
        if (playerId == null) throw new IllegalArgumentException("Bid turn: Player cannot be null");
        if (bidType == null) throw new IllegalArgumentException("Bid turn: bidType cannot be null");
    }
}
