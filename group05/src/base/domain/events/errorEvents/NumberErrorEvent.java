package base.domain.events.errorEvents;

import base.domain.events.GameEvent;
import java.util.function.Predicate;

public record NumberErrorEvent(String errorMessage, Predicate<Integer> validationLogic) implements GameEvent<Integer> {

    @Override
    public Class<Integer> getInputType() {
        return Integer.class;
    }

    @Override
    public boolean isValid(Integer input) {
        // Use the logic passed from the original event
        return validationLogic.test(input);
    }

    @Override
    public boolean needsInput() {
        return true;
    }
}