package base.domain.events.menuEvents;

import base.domain.events.GameEvent;

public record AmountOfBotsEvent() implements GameEvent {
    private String renderAmountOfBotsEvent(){
        return "How many bots will be playing? (0-3):";
    }
    //lower : 0
    //higher : 3
}
