package base.domain.commands;



public sealed interface GameCommand permits BidCommand, ContinueCommand, NumberCommand, PlaceBidCommand, PlayerListCommand, StartGameCommand, SuitCommand, TextCommand {}