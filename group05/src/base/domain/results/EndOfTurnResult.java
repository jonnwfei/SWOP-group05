package base.domain.results;

import base.domain.card.Card;

public record EndOfTurnResult (String name, Card card) implements GameResult{
}
