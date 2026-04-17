package base.domain.commands;

import base.domain.player.PlayerId;

import java.util.List;

public record PlayerListCommand(List<PlayerId> playerIds) implements GameCommand {
    public PlayerListCommand {
        if (playerIds == null) {
            throw new IllegalArgumentException("players cannot be null");
        }
    }
}
