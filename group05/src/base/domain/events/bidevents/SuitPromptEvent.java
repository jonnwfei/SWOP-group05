package base.domain.events.bidevents;

import base.domain.bid.BidType;
import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.events.GameEvent;

/**
 * A Data Transfer Object (DTO) representing a follow-up request for a suit selection.
 * * This event is triggered when a player selects a bid type that requires a specific
 * trump suit to be declared (e.g., Abondance or Solo). The Domain holds the
 * {@code pendingType} in memory while waiting for this response.
 *
 * @param playerName  The name of the player currently being prompted to choose a suit.
 * @param pendingType The specific {@link BidType} the player just selected, used to
 * contextualize the UI prompt (e.g., "You chose Solo. Pick a suit:").
 * @param suits       The array of all available {@link Suit} options (usually Hearts,
 * Clubs, Diamonds, Spades).
 */
public record SuitPromptEvent(String playerName, BidType pendingType, Suit[] suits) implements GameEvent {
}
