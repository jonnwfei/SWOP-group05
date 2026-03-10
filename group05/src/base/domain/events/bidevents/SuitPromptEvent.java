package base.domain.events.bidevents;

import base.domain.bid.BidType;
import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.events.GameEvent;

public record SuitPromptEvent(String playerName, BidType pendingType, Suit[] suits) implements GameEvent {
}
