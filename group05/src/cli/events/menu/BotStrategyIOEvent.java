package cli.events.menu;

import cli.events.IOEvent;

public record BotStrategyIOEvent(int botIndex) implements IOEvent {
    public boolean needsInput() { return true; }
    public boolean getContinue() { return true; }
}
