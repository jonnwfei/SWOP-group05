package base.domain.events.menuEvents;

import base.domain.events.GameEvent;
import base.domain.player.Player;

import java.util.List;

public record PrintNamesEvent(List<String> playerNames) implements GameEvent {
    // Defensive copy to prevent external modification
    public PrintNamesEvent {
        playerNames = List.copyOf(playerNames);
    }
}
