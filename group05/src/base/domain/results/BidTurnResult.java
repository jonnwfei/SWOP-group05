package base.domain.results;

import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.player.Player;

import java.util.List;

public record BidTurnResult(
        String playerName,
        Suit trumpSuit,
        BidType currentHighestBid,
        List<BidType> availableBids,
        List<Card> hand,
        Player player
) implements GameResult {

    public BidTurnResult {
        if (playerName == null || playerName.isBlank()) {
            throw new IllegalArgumentException("playerName cannot be null or blank");
        }
        if (availableBids == null || availableBids.isEmpty()) {
            throw new IllegalArgumentException("availableBids cannot be null or empty");
        }
        if (hand == null || hand.isEmpty()) {
            throw new IllegalArgumentException("hand cannot be null or empty");
        }
        if (player == null) {
            throw new IllegalArgumentException("player cannot be null");
        }

        availableBids = List.copyOf(availableBids);
        hand = List.copyOf(hand);
    }
}