package cli.events.BidEvents;

import cli.events.IOEvent;

public record BiddingCompletedIOEvent() implements IOEvent {
    public boolean needsInput() { return false; }
    public boolean getContinue() { return false; } // exits state loop
}