package base.domain.results;

import base.domain.card.Card;

// TODO: winner mag niet null zijn maar in playState word hij wel op null gezet, maar gaat dit nooit door, oppassen
public record EndOfTrickResult(String name, Card card, String winner) implements GameResult {

    public EndOfTrickResult {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        if (card == null) {
            throw new IllegalArgumentException("Card cannot be null");
        }
        if (winner == null || winner.isBlank()) {
            throw new IllegalArgumentException("Winner cannot be null or blank");
        }
    }
}