package base.domain.events.playevents;

import base.domain.player.Player;
import base.domain.events.GameEvent;

public record InitiateTurnEvent(String playerName) implements GameEvent {
}
