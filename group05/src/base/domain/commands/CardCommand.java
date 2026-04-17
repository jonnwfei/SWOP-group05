package base.domain.commands;

import base.domain.card.Card;

public record CardCommand(Card card) implements GameCommand {
    public CardCommand(Card card) {
        if (card == null) {
            throw new IllegalArgumentException("card cannot be null");
        }
        this.card = card;
    }
}

