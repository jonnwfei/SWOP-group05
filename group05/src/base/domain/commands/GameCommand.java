package base.domain.commands;

public sealed interface GameCommand permits BidCommand, CardCommand, NumberCommand, PlaceBidCommand, PlayerListCommand, RoundCommand, StartGameCommand, SuitCommand, TextCommand {}