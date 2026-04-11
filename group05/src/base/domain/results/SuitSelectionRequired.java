package base.domain.results;

import base.domain.bid.BidType;
import base.domain.card.Suit;

public record SuitSelectionRequired(
        String playerName,
        BidType bidType,
        Suit[] suits
) implements GameResult {}
