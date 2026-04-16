package base.domain.results;

import java.util.List;

public record ParticipatingPlayersResult(
        List<String> playerNames,
        boolean multiSelect
) implements GameResult {

    public ParticipatingPlayersResult {
        if (playerNames == null || playerNames.isEmpty() || playerNames.contains(null)) {
            throw new IllegalArgumentException("PlayerNames cannot be null or empty or contain null objects");
        }

        playerNames = List.copyOf(playerNames);
    }
}

