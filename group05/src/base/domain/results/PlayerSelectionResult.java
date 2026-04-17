package base.domain.results;

import base.domain.bid.BidType;
import base.domain.player.Player;

import java.util.List;

public record PlayerSelectionResult(
        List<Player> players,
        boolean multiSelect,
        BidType type
) implements GameResult {

    public PlayerSelectionResult {
        if (players == null || players.isEmpty()) {
            throw new IllegalArgumentException("players cannot be null or empty");
        }
        if (type == null){
            throw new IllegalArgumentException("bidtype cannot be null");
        }
        players = List.copyOf(players);
    }
}