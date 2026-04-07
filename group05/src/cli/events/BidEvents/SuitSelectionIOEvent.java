package cli.events.BidEvents;

import base.domain.results.SuitSelectionRequired;
import cli.events.IOEvent;

public record SuitSelectionIOEvent(SuitSelectionRequired data) implements IOEvent {
    public boolean needsInput() { return true; }
    public boolean getContinue() { return true; }
}