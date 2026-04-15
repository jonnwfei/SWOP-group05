package cli.events.menu;

import cli.events.IOEvent;

import java.util.List;

public record PrintNamesIOEvent(List<String> playerNames) implements IOEvent {
    public boolean needsInput() { return false; }
    public boolean getContinue() { return false; } // done → nextState()
}
