package base.domain.events.menuEvents;

import base.domain.events.GameEvent;

/**
 * Prompts for a save description.
 */
public record SaveDescriptionEvent(String contextLabel) implements GameEvent<String> {
    @Override
    public Class<String> getInputType() {
        return String.class;
    }

    @Override
    public boolean isValid(String input) {
        return input != null && !input.isBlank();
    }

    @Override
    public boolean needsInput() {
        return true;
    }
}

