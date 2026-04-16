package base.domain.commands;



public sealed interface GameCommand permits BidCommand, CardCommand, NumberCommand, PlaceBidCommand, PlayerListCommand, StartGameCommand, SuitCommand, TextCommand {}