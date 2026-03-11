package base.domain.events.menuEvents;

import base.domain.events.GameEvent;

public record WelcomeMenuEvent() implements GameEvent<Integer> {
    @Override
    public Class<Integer> getInputType() {
        return Integer.class;
    }

    @Override
    public boolean isValid(Integer input) {
        return input >= 1 && input <= 2;
    }
    @Override
    public boolean needsInput(){
        return true;
    }
}
