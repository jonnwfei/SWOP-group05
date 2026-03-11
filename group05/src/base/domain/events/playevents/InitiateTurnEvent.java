package base.domain.events.playevents;

import base.domain.player.Player;
import base.domain.events.GameEvent;

public record InitiateTurnEvent(String playerName) implements GameEvent<String> {
    @Override
    public Class<String> getInputType() {
        return String.class;
    }

    @Override
    public boolean isValid(String input) {
        return input != null;
    }
    @Override
    public boolean needsInput(){
        return true;
    }
}
