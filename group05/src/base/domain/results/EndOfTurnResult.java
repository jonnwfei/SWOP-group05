package base.domain.results;

import base.domain.card.Card;

import java.util.Objects;

public record EndOfTurnResult(String name, Card card) implements GameResult {

    public EndOfTurnResult {
        Objects.requireNonNull(name);
        Objects.requireNonNull(card);
    }
}