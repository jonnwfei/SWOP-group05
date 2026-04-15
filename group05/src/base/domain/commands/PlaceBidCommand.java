package base.domain.commands;

import base.domain.bid.BidType;
import base.domain.results.GameResult;
import base.domain.states.BidState;
import base.domain.states.State;

public record PlaceBidCommand(BidType bidType) implements GameCommand { }