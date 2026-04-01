package base.domain.events.menuEvents;

import base.domain.events.GameEvent;

public record PlayerNameEvent(int playerIndex) implements GameEvent<String> {
    @Override
    public Class<String> getInputType() {
        return String.class;
    }

    @Override
    public boolean isValid(String input) {
        return !input.isEmpty();
    }

    @Override
    public boolean needsInput() {
        return true;
    }
}