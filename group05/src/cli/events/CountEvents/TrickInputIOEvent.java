package cli.events.CountEvents;

import cli.events.IOEvent;

public record TrickInputIOEvent() implements IOEvent {
    public boolean needsInput() { return true; }
    public boolean getContinue() { return true; }
}
