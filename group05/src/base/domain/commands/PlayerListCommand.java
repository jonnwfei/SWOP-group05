package base.domain.commands;

import base.domain.bid.BidType;
import base.domain.player.Player;

import java.util.List;

public record PlayerListCommand(List<Player> players) implements GameCommand {
    public PlayerListCommand {
        if (players == null) {
            throw new IllegalArgumentException("players cannot be null");
        }
    }
}
