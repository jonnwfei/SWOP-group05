package base.domain.actions;

/**
 * Sealed protocol for UI inputs processed via {@code executeState(GameAction)}.
 *
 * <p>Keeping this closed allows exhaustive switch handling in states.
 *
 * @author Tommy
 * @since 10/03/2026
 */
public sealed interface GameAction permits ContinueAction, NumberAction, NumberListAction, TextAction {}