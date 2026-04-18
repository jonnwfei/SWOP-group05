package base.domain.results;

import base.domain.player.PlayerId;
import base.domain.trick.Trick;

import java.util.Map;

public record TrickHistoryResult(
        Trick trick,
        Map<PlayerId, String> playerNames
) implements GameResult {

    public TrickHistoryResult {
        if (trick == null) {
            throw new IllegalArgumentException("trick cannot be null");
        }
        if (playerNames == null || playerNames.containsKey(null) || playerNames.containsValue(null)) {
            throw new IllegalArgumentException("playerNames cannot be null or contain null objects");
        }

        // Defensive copy
        playerNames = Map.copyOf(playerNames);
    }
}