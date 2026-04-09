package base.domain.events.menuEvents;

import base.domain.events.GameEvent;
import base.domain.round.Round;

import java.util.List;

public record RemoveRoundEvent(List<Round> rounds) implements GameEvent<Integer> {
    @Override
    public Class<Integer> getInputType() {
        return Integer.class;
    }

    @Override
    public boolean isValid(Integer input) {
        return input >= 0 && input < rounds.size();
    }

    @Override
    public boolean needsInput() {
        return true;
    }
}