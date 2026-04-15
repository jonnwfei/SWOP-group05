package cli.events.BidEvents;

import base.domain.results.BidTurnResult;
import cli.events.IOEvent;

public record BidTurnIOEvent(BidTurnResult data) implements IOEvent {
    public boolean needsInput() { return true; }
    public boolean getContinue() { return true; }
}