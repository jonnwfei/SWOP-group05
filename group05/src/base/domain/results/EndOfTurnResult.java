package base.domain.results;

import base.domain.card.Card;


public record EndOfTurnResult(String name, Card card) implements GameResult {

    public EndOfTurnResult {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name cannot be null or blank");
        }
        if (card == null) {
            throw new IllegalArgumentException("card cannot be null");
        }
    }
}