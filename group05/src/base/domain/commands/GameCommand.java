package base.domain.commands;



public sealed interface GameCommand permits BidCommand, CardCommand, ContinueCommand, NumberCommand, PlaceBidCommand, PlayerListCommand, StartGameCommand, SuitCommand, TextCommand {}