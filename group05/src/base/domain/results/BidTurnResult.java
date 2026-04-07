package base.domain.results;

import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Suit;

import java.util.List;

public record BidTurnResult(
        String playerName,
        Suit trumpSuit,
        BidType currentHighestBid,
        BidType[] availableBids,
        List<Card> hand
) implements GameResult {}