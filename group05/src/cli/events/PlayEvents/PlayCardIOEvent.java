package cli.events.PlayEvents;

import base.domain.results.PlayCardResult;
import cli.events.IOEvent;

public record PlayCardIOEvent(PlayCardResult data) implements IOEvent {
    public boolean needsInput() { return true; }
    public boolean getContinue() { return true; }
}