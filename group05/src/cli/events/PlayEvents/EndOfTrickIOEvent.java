package cli.events.PlayEvents;

import base.domain.results.EndOfTrickResult;
import cli.events.IOEvent;

public record EndOfTrickIOEvent(EndOfTrickResult data) implements IOEvent {
    public boolean needsInput() { return false; }
    public boolean getContinue() { return true; }
}
