package cli.events.menu;

import cli.events.IOEvent;

import java.util.List;

public record LoadSaveIOEvent(List<String> availableSaves) implements IOEvent {
    public boolean needsInput() { return true; }
    public boolean getContinue() { return true; }
}
