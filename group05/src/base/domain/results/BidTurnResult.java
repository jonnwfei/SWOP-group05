package base.domain.results;

import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.player.Player;

import java.util.List;
import java.util.Objects;

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
            throw new IllegalArgumentException("PlayerName cannot be null or blank");
        }
        if (availableBids == null || availableBids.isEmpty() || availableBids.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("AvailableBids cannot be null or empty or contain null elements");
        }
        if (hand == null || hand.isEmpty() ||  hand.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Hand cannot be null or empty or contain null elements");
        }
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }

        availableBids = List.copyOf(availableBids);
        hand = List.copyOf(hand);
    }
}