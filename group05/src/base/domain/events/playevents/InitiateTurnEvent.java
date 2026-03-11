package base.domain.events.playevents;

import base.domain.player.Player;
import base.domain.events.GameEvent;

public record InitiateTurnEvent(String playerName) implements GameEvent<Void> {
    @Override
    public Class<Void> getInputType() {
        return Void.class;
    }

    @Override
    public boolean isValid(Void input) {
        return input != null;
    }

    @Override
    public boolean needsInput(){
        return true;
    }
}
