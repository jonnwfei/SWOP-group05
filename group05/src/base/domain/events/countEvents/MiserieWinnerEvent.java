package base.domain.events.countEvents;

import base.domain.events.GameEvent;
import base.domain.player.Player;

import java.util.List;

public record MiserieWinnerEvent(List<String> playerNames) implements GameEvent {
    public MiserieWinnerEvent {
        playerNames = List.copyOf(playerNames); // Defensive copy
    }
}
