package base.domain.actions;

/**
 * Action carrying raw text input.
 * @param text The string entered by the user.
 */
public record TextAction(String text) implements GameAction {}
