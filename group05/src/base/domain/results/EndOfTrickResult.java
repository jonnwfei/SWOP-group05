package base.domain.results;

import base.domain.card.Card;

import java.util.Objects;

public record EndOfTrickResult(String name, Card card, String winner) implements GameResult {

    public EndOfTrickResult {
        Objects.requireNonNull(name);
        Objects.requireNonNull(card);
    }
}