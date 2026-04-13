package base.domain.results;

import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Suit;

import java.util.List;
import java.util.Objects;

public record BidTurnResult(
        String playerName,
        Suit trumpSuit,
        BidType currentHighestBid,
        List<BidType> availableBids,
        List<Card> hand
) implements GameResult {

    public BidTurnResult {
        Objects.requireNonNull(playerName);
        Objects.requireNonNull(availableBids);
        Objects.requireNonNull(hand);

        availableBids = List.copyOf(availableBids);
        hand = List.copyOf(hand);
    }
}