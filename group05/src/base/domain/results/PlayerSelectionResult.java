package base.domain.results;

import java.util.List;

public record PlayerSelectionResult(List<String> playerNames, String message, boolean multiSelect) implements GameResult {}

