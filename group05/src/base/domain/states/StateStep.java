package base.domain.states;

import base.domain.results.GameResult;
import java.util.Objects;

/**
 * Output of a state execution step: an optional renderable result and
 * whether the state machine should transition afterward.
 */
public record StateStep(GameResult result, boolean shouldTransition) {

    public static StateStep stay(GameResult result) {
        return new StateStep(Objects.requireNonNull(result), false);
    }

    public static StateStep transition(GameResult result) {
        return new StateStep(Objects.requireNonNull(result), true);
    }

    public static StateStep transitionWithoutResult() {
        return new StateStep(null, true);
    }

    public boolean hasResult() {
        return result != null;
    }
}