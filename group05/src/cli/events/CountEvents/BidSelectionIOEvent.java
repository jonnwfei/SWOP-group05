package cli.events.CountEvents;

import base.domain.bid.BidType;
import cli.events.IOEvent;

public record BidSelectionIOEvent(BidType[] bidTypes) implements IOEvent {
    public boolean needsInput() { return true; }
    public boolean getContinue() { return true; }
}
