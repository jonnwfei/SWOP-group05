package base.domain.events.playevents;

import base.domain.card.Card;
import base.domain.player.Player;
import base.domain.events.GameEvent;

public record EndOfTrickEvent(String playerName, Card card) implements GameEvent<String> {
    @Override
    public Class<String> getInputType() {
        return String.class;
    }

    @Override
    public boolean isValid(String input) {
        return true;
    }
    
    @Override
    public boolean needsInput(){
        return false;
    }
}
