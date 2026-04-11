package base.domain.results;

import base.domain.bid.BidType;

public record BidSelectionResult(BidType[] availableBids, java.util.List<base.domain.player.Player> players) implements GameResult {}