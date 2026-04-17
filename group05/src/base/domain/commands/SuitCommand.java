package base.domain.commands;

import base.domain.card.Suit;

public record SuitCommand(Suit suit) implements GameCommand {
    public SuitCommand(Suit suit) {
        if (suit == null) {
            throw new IllegalArgumentException("suit cannot be null");
        }
        this.suit = suit;
    }
}