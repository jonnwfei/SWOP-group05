package base.domain.events.menuEvents;

import base.domain.events.GameEvent;

public record PlayerNameEvent(int index) implements GameEvent {
    private String renderPlayerNameEvent(){
    return "Give the name of player" + index + ": ";
    }
    //geen voorwaarden op
}
