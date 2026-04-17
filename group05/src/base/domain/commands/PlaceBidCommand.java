package base.domain.commands;

import base.domain.bid.BidType;

public record PlaceBidCommand(BidType bidType) implements GameCommand {
    public PlaceBidCommand {
        if (bidType == null) {
            throw new IllegalArgumentException("bidType cannot be null");
        }
    }
}