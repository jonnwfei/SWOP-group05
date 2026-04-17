package base.domain.results;

import base.domain.card.Card;

public record EndOfRoundResult(String name, Card card) implements GameResult {

    public EndOfRoundResult {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        // card may be null (you already use it like that)
    }
}
