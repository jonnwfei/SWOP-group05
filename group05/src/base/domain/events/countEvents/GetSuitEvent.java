package base.domain.events.countEvents;

import base.domain.events.GameEvent;

public record GetSuitEvent() implements GameEvent<Integer> {

    @Override
    public Class<Integer> getInputType() {
        return Integer.class;
    }

    @Override
    public boolean isValid(Integer input) {
        if (input < 1 || input > 16) {
            return false;
        }
        else{
            return true;
        }
    }
    @Override
    public boolean needsInput(){
        return true;
    }
}