package base.domain.events.countEvents;

import base.domain.events.GameEvent;

import java.util.List;

public record ScoreBoardEvent(List<String> playerNames, List<Integer> scores) implements GameEvent<Integer> {
    @Override
    public Class<Integer> getInputType() {
        return Integer.class;
    }

    @Override
    public boolean isValid(Integer input) {
        return input >= 1 && input <= 3;
    }

    @Override
    public boolean needsInput() {
        return true;
    }
}