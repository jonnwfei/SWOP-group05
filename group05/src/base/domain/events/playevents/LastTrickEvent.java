package base.domain.events.playevents;

import base.domain.events.GameEvent;
import base.domain.trick.Trick;
import base.domain.trick.Turn;

public record LastTrickEvent(Trick trick) implements GameEvent {
}

