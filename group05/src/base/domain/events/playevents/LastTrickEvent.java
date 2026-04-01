package base.domain.events.playevents;

import base.domain.events.GameEvent;
import base.domain.trick.Trick;

public record LastTrickEvent(Trick trick) implements GameEvent<String> {
    @Override
    public Class<String> getInputType() {
        return String.class;
    }

    @Override
    public boolean isValid(String input) {
        return true;
    }

    @Override
    public boolean needsInput() {
        return false;
    }
}
