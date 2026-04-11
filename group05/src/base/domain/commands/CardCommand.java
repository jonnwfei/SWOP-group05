package base.domain.commands;

import base.domain.card.Card;

public record CardCommand(Card card) implements GameCommand {
}

