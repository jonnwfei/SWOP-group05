package base.domain.results;

import java.util.List;

public record ScoreBoardResult(List<String> names, List<Integer> scores) implements GameResult {}
