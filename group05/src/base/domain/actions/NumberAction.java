package base.domain.actions;

// For when the user types a number (e.g., choosing a bid, suit, or card index)
public record NumberAction(int value) implements GameAction {}
