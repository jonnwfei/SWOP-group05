package base.domain.results;

import base.domain.card.Card;

public record EndOfTrickResult(String name, Card card, String winner) implements GameResult {
}
