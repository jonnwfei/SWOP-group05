package base.domain.commands;



public sealed interface GameCommand permits BidCommand, ContinueCommand, PlaceBidCommand, StartGameCommand, SuitCommand {}