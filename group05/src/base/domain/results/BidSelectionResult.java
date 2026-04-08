package base.domain.results;

import base.domain.bid.BidType;

public record BidSelectionResult(BidType[] availableBids) implements GameResult {}