package base.domain.events.countEvents;

import base.domain.events.GameEvent;

public record EndOfCountStateEvent() implements GameEvent<String> {
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
