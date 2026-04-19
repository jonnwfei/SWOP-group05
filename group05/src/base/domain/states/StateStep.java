package base.domain.states;

import base.domain.results.GameResult;

/**
 * Output of a state execution step: an optional renderable result and
 * whether the state machine should transition afterward.
 */
public record StateStep(GameResult result, boolean shouldTransition) {

    /**
     * Creates a StateStep representing a stay in the current state with a result.
     * @param result The result to be rendered.
     * @return A StateStep indicating to stay in the current state with the provided result.
     * @throws IllegalArgumentException if result is null
     */
    public static StateStep stay(GameResult result) {
        if (result == null) throw new IllegalArgumentException("Result must not be null for a stay step");
        return new StateStep(result, false);
    }

    /**
     * Creates a StateStep representing a transition to the next state with a result.
     * @param result The result to be rendered.
     * @return A StateStep indicating to transition to the next state with the provided result.
     * @throws IllegalArgumentException if result is null
     */
    public static StateStep transition(GameResult result) {
        if (result == null) throw new IllegalArgumentException("Result must not be null for a transition step");
        return new StateStep(result, true);
    }

    /**
     * Creates a StateStep representing a transition to the next state without any result.
     * @return A StateStep indicating to transition to the next state without any result.
     */
    public static StateStep transitionWithoutResult() {
        return new StateStep(null, true);
    }

    /**
     * Retrieves whether this StateStep has a non-null result to be rendered.
     * @return true if there is a result to be rendered, false otherwise.
     */
    public boolean hasResult() {
        return result != null;
    }
}