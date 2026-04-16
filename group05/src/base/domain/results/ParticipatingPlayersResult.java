package base.domain.results;

import java.util.List;

public record ParticipatingPlayersResult(
        List<String> playerNames,
        boolean multiSelect
) implements GameResult {

    public ParticipatingPlayersResult {
        if (playerNames == null || playerNames.isEmpty()) {
            throw new IllegalArgumentException("playerNames cannot be null or empty");
        }

        playerNames = List.copyOf(playerNames);
    }
}

