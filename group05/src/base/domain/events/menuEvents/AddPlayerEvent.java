package base.domain.events.menuEvents;

import base.domain.events.GameEvent;

public record AddPlayerEvent() implements GameEvent<String> {
    @Override
    public Class<String> getInputType() {
        return String.class;
    }

    @Override
    public boolean isValid(String input) {
        return input != null && !input.trim().isEmpty();
    }

    @Override
    public boolean needsInput() {
        return true;
    }
}
