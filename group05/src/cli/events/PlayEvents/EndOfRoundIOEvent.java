package cli.events.PlayEvents;

import base.domain.results.EndOfRoundResult;
import cli.events.IOEvent;

public record EndOfRoundIOEvent(EndOfRoundResult data) implements IOEvent {
    public boolean needsInput() { return false; }
    public boolean getContinue() { return false; }
}
