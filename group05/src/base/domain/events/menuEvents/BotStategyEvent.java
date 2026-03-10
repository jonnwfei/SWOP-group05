package base.domain.events.menuEvents;

import base.domain.events.GameEvent;

public record BotStategyEvent(int i) implements GameEvent {
    private String BotStrategyEvent(){
        return "Which strategy should bot " + i + " use?\n(1) High Bot\n(2) Low Bot\n";
    }
}
//lower 1
//higher 2