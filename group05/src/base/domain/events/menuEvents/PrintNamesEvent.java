package base.domain.events.menuEvents;

import base.domain.events.GameEvent;

import java.util.List;

public record PrintNamesEvent(List<String> playerNames) implements GameEvent<String> {
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
