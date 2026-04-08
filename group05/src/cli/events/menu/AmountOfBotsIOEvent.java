package cli.events.menu;

import cli.events.IOEvent;

public record AmountOfBotsIOEvent() implements IOEvent {
    public boolean needsInput() { return true; }
    public boolean getContinue() { return true; }
}
