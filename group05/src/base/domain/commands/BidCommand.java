package base.domain.commands;

import base.domain.bid.BidType;

public record BidCommand(BidType bid) implements GameCommand {}