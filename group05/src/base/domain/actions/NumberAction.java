package base.domain.actions;

/**
 * Action carrying a single menu choice or index.
 * @param value The integer selected by the user.
 * @author Tommy
 * @since 10/03/2026
 * */
public record NumberAction(int value) implements GameAction {}
