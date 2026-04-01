package base.domain.events.bidevents;

import base.domain.bid.BidType;
import base.domain.card.Suit;
import base.domain.events.GameEvent;

/**
 * A Data Transfer Object (DTO) dispatched when a player selects a bid that
 * requires a custom trump suit declaration.
 * * This event acts as a "sub-menu" in the bidding process. After selecting
 * a {@link BidType} like SOLO or ABONDANCE, the domain enters a pending state
 * and issues this event to determine which suit will become the trump.
 *
 * @param playerName  The name of the player currently making the selection.
 * @param pendingType The specific {@link BidType} that was previously chosen
 *                    and is currently awaiting a suit assignment.
 * @param suits       An array of available {@link Suit} values (Hearts, Clubs,
 *                    Diamonds, Spades) to be rendered as menu options.
 */
public record SuitPromptEvent(String playerName, BidType pendingType, Suit[] suits) implements GameEvent<Integer> {
    @Override
    public Class<Integer> getInputType() {
        return Integer.class;
    }

    @Override
    public boolean isValid(Integer input) {
        return input >= 1 && input <= suits.length;
    }

    @Override
    public boolean needsInput() {
        return true;
    }
}
