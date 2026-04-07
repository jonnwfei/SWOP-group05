package base.domain.commands;



public sealed interface GameCommand permits BidCommand, SuitCommand, ContinueCommand {}