package base.domain.events.menuEvents;

import base.domain.events.GameEvent;

public record BotStrategyEvent(int botIndex) implements GameEvent {}