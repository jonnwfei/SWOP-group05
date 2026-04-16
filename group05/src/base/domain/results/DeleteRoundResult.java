package base.domain.results;

import base.domain.round.Round;

import java.util.List;

public record DeleteRoundResult(List<Round> rounds) implements GameResult{
}
