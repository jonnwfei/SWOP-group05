package cli.events;

public interface IOEvent {
    boolean needsInput();
    boolean getContinue(); // false = exit state loop → nextState()
}