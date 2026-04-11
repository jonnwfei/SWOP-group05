package cli.events.PlayEvents;

import base.domain.results.*;
import cli.events.IOEvent;

public record EndOfTurnIOEvent(EndOfTurnResult data) implements IOEvent {
    public boolean needsInput() { return false; }
    public boolean getContinue() { return true; }
}

