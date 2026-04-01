package base.domain.events.menuEvents;

import base.domain.events.GameEvent;

public record AmountOfBotsEvent() implements GameEvent<Integer> {
    @Override
    public Class<Integer> getInputType() {
        return Integer.class;
    }

    @Override
    public boolean isValid(Integer input) {
        if (input < 0 || input > 3) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean needsInput() {
        return true;
    }
}