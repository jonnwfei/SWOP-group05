package base.domain.results;

import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.player.Player;
import java.util.List;

public sealed interface BidResults extends GameResult {

    record BidTurnResult(
            String playerName,
            Suit trumpSuit,
            BidType currentHighestBid,
            List<BidType> availableBids,
            List<Card> hand,
            Player player
    ) implements BidResults {
        public BidTurnResult {
            if (playerName == null || playerName.isBlank())
                throw new IllegalArgumentException("playerName cannot be null or blank.");
            if (trumpSuit == null)
                throw new IllegalArgumentException("trumpSuit cannot be null.");
            if (availableBids == null || availableBids.isEmpty())
                throw new IllegalArgumentException("availableBids cannot be null or empty.");
            if (hand == null)
                throw new IllegalArgumentException("hand cannot be null.");
            if (player == null)
                throw new IllegalArgumentException("player cannot be null.");
            availableBids = List.copyOf(availableBids);
            hand = List.copyOf(hand);
        }
    }

    record SuitSelectionRequired(
            String playerName,
            BidType pendingBid,
            Suit[] availableSuits
    ) implements BidResults {
        public SuitSelectionRequired {
            if (playerName == null || playerName.isBlank())
                throw new IllegalArgumentException("playerName cannot be null or blank.");
            if (pendingBid == null)
                throw new IllegalArgumentException("pendingBid cannot be null.");
            if (availableSuits == null || availableSuits.length == 0)
                throw new IllegalArgumentException("availableSuits cannot be null or empty.");
        }
    }

    record ProposalRejected(String playerName) implements BidResults {
        public ProposalRejected {
            if (playerName == null || playerName.isBlank())
                throw new IllegalArgumentException("playerName cannot be null or blank.");
        }
    }

    record BiddingCompleted() implements BidResults {}
}