package cli.events.BidEvents;

import base.domain.results.ProposalRejected;
import cli.events.IOEvent;

public record ProposalRejectedIOEvent(ProposalRejected data) implements IOEvent {
    public boolean needsInput() { return true; }
    public boolean getContinue() { return true; }
}