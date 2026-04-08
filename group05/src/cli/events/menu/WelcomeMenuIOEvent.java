package cli.events.menu;

import cli.events.IOEvent;

public record WelcomeMenuIOEvent() implements IOEvent {
    public boolean needsInput() { return true; }
    public boolean getContinue() { return true; }
}

