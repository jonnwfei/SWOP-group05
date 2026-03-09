package base.domain.events.bidevents;

import base.domain.bid.BidType;
import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.events.GameEvent;

public record SuitPromptEvent(Player currentPlayer, BidType pendingType, Suit[] suits) implements GameEvent {
}
