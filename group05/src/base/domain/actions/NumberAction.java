package base.domain.actions;

/**
 * Action carrying a single menu choice or index.
 * @param value The integer selected by the user.
 */
public record NumberAction(int value) implements GameAction {}
