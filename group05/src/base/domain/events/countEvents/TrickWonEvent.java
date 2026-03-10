package base.domain.events.countEvents;

import base.domain.events.GameEvent;

public record TrickWonEvent() implements GameEvent {
    private String renderTrickWonEvent(){
        return "How many tricks did the player(s) win?";
    }
}
