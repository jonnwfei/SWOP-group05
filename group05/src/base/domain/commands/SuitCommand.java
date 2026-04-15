package base.domain.commands;

import base.domain.card.Suit;

public record SuitCommand(Suit suit) implements GameCommand {}