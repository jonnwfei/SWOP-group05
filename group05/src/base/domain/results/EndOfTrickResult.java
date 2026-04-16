package base.domain.results;

import base.domain.card.Card;


public record EndOfTrickResult(String name, Card card, String winner) implements GameResult {

    public EndOfTrickResult {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name cannot be null or blank");
        }
        if (card == null) {
            throw new IllegalArgumentException("card cannot be null");
        }
        if (winner == null || winner.isBlank()) {
            throw new IllegalArgumentException("winner cannot be null or blank");
        }
    }
}