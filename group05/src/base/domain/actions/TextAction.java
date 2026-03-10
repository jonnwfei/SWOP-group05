package base.domain.actions;

// For when the user types letters (e.g., entering their player name)
public record TextAction(String text) implements GameAction {}
