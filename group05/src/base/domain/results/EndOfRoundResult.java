package base.domain.results;

import base.domain.card.Card;

public record EndOfRoundResult(String name, Card card) implements GameResult{
}
