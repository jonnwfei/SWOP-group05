package cli.events.CountEvents;

import cli.events.IOEvent;

public record SaveDescriptionIOEvent() implements IOEvent {
    public boolean needsInput() { return true; }
    public boolean getContinue() { return true; }
}
