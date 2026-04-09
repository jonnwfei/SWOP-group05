package base.domain.results;

import java.util.List;

public record ParticipatingPlayersResult(List<String> playerNames, boolean multiSelect) implements GameResult {}


