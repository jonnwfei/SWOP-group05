package base.domain.results;

import java.util.List;

public record ScoreBoardResult(
        List<String> names,
        List<Integer> scores
) implements GameResult {

    public ScoreBoardResult {
        if (names == null || names.isEmpty()) {
            throw new IllegalArgumentException("names cannot be null or empty");
        }
        if (scores == null || scores.isEmpty()) {
            throw new IllegalArgumentException("scores cannot be null or empty");
        }
        if (names.size() != scores.size()) {
            throw new IllegalArgumentException("names and scores must have same size");
        }

        names = List.copyOf(names);
        scores = List.copyOf(scores);
    }
}