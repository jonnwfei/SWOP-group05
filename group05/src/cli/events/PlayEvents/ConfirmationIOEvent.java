package cli.events.PlayEvents;

import cli.events.IOEvent;

public record ConfirmationIOEvent(String playerName) implements IOEvent {
    @Override public boolean needsInput() { return true; }
    @Override public boolean getContinue() { return true; }
}
