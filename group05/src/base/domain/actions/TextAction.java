package base.domain.actions;

/**
 * Action carrying raw text input.
 * @param text The string entered by the user.
 * @author Tommy
 * @since 10/03/2026
 */
public record TextAction(String text) implements GameAction {}
