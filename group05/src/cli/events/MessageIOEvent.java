package cli.events;

public record MessageIOEvent (String text) implements IOEvent{
    @Override
    public boolean needsInput() {
        return false;
    }

    @Override
    public boolean getContinue() {
        return false;
    }
}
