package cli.events;

public record MessageIOEvent(String text) implements IOEvent {
    public boolean needsInput() {
        return false;
    }
}
