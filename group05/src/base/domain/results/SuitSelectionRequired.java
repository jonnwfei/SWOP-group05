package base.domain.results;

import base.domain.bid.BidType;
import base.domain.card.Suit;

public record SuitSelectionRequired(
        String playerName,
        BidType bidType,
        Suit[] suits
) implements GameResult {

    public SuitSelectionRequired {
        if (playerName == null || playerName.isBlank()) {
            throw new IllegalArgumentException("playerName cannot be null or blank");
        }
        if (bidType == null) {
            throw new IllegalArgumentException("bidType cannot be null");
        }
        if (suits == null || suits.length == 0) {
            throw new IllegalArgumentException("suits cannot be null or empty");
        }

        suits = suits.clone();
    }
}
