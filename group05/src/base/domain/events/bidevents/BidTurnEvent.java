package base.domain.events.bidevents;

import base.domain.bid.BidType;
import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.events.GameEvent;

public record BidTurnEvent(Player currentPlayer, Suit dealtTrump, BidType currentHighestBidType, BidType[] bidTypes) implements GameEvent {
}
