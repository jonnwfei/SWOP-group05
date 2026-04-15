package cli.events.PlayEvents;

import base.domain.results.TrickHistoryResult;
import cli.events.IOEvent;

public record TrickHistoryIOEvent(TrickHistoryResult data) implements IOEvent {
    public boolean needsInput() { return false; }
    public boolean getContinue() { return true; }
}
