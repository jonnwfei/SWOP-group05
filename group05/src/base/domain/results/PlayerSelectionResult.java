package base.domain.results;

import base.domain.player.Player;

import java.util.List;

public record PlayerSelectionResult(
        List<Player> players,
        boolean multiSelect
) implements GameResult {

    public PlayerSelectionResult {
        if (players == null || players.isEmpty()) {
            throw new IllegalArgumentException("players cannot be null or empty");
        }

        players = List.copyOf(players);
    }
}