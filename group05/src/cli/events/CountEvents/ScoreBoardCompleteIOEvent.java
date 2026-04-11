package cli.events.CountEvents;

import cli.events.IOEvent;

public record ScoreBoardCompleteIOEvent() implements IOEvent {
    public boolean needsInput() { return false; }
    public boolean getContinue() { return false; } // exits inner loop
}
