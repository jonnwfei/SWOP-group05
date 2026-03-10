package base.domain.events.menuEvents;

import base.domain.events.GameEvent;

public record PlayerNameEvent(int playerIndex) implements GameEvent {}