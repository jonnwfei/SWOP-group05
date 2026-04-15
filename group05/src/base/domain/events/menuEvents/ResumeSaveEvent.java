package base.domain.events.menuEvents;

import base.domain.events.GameEvent;

import java.util.List;

/**
 * Prompts the user to choose one of the available saves.
 */
public record ResumeSaveEvent(List<String> descriptions) implements GameEvent<Integer> {
    @Override
    public Class<Integer> getInputType() {
        return Integer.class;
    }

    @Override
    public boolean isValid(Integer input) {
        return input >= 1 && input <= descriptions.size();
    }

    @Override
    public boolean needsInput() {
        return true;
    }
}

