package base.domain.events.bidevents;

import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.events.GameEvent;

import java.util.List;

/**
 * A Data Transfer Object (DTO) that captures the complete state of a bidding
 * turn.
 * * This event provides the IO with all necessary context to render a player's
 * bidding interface, including their current hand, the suit of the dealt trump,
 * and a list of available bidding options.
 *
 * @param playerName            The name of the player whose turn it is to bid.
 * @param dealtTrump            The suit of the current trump,
 * @param currentHighestBidType The strongest bid placed so far in the current
 *                              bidding phase
 * @param bidTypes              An array of all possible {@link BidType} values.
 * @param playerHand            The 13 cards currently held by the bidding
 *                              player.
 */
public record BidTurnEvent(String playerName, Suit dealtTrump, BidType currentHighestBidType, BidType[] bidTypes,
        List<Card> playerHand) implements GameEvent<Integer> {
    @Override
    public Class<Integer> getInputType() {
        return Integer.class;
    }

    @Override
    public boolean isValid(Integer input) {
        return input >= 1 && input <= bidTypes.length;
    }

    @Override
    public boolean needsInput() {
        return true;
    }

}
