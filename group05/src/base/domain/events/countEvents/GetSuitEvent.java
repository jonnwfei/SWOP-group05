package base.domain.events.countEvents;

import base.domain.events.GameEvent;

public record GetSuitEvent() implements GameEvent {
    private String renderGetSuitEvent(){
        return "What Suit is the trump suit?\n(1) Hearts (2) Clubs (3) Diamonds (4) Spades";
    }
}
