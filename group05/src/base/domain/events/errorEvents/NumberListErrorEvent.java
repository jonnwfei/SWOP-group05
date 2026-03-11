package base.domain.events.errorEvents;

import base.domain.events.GameEvent;
import java.util.ArrayList;
import java.util.function.Predicate;

public record NumberListErrorEvent(
        Predicate<ArrayList<Integer>> validationLogic
) implements GameEvent<ArrayList<Integer>> {

    @Override
    public Class<ArrayList<Integer>> getInputType() {
        return (Class<ArrayList<Integer>>) (Class<?>) ArrayList.class;
    }

    @Override
    public boolean isValid(ArrayList<Integer> input) {
        // Use the passed-in function to validate!
        return validationLogic.test(input);
    }

    @Override
    public boolean needsInput() {
        return true;
    }
}