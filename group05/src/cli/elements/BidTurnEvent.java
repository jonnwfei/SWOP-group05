package cli.elements;

import base.domain.bid.BidType;
import base.domain.card.Suit;
import base.domain.player.Player;

public record BidTurnEvent(Player currentPlayer, Suit dealtTrump, BidType currentHighestBid) implements GameEvent {
}
