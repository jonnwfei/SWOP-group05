package cli.events.menu;

import cli.events.IOEvent;

public record AddPlayerIOEvent() implements IOEvent {
    @Override
    public boolean needsInput() {
        return true;
    }

    @Override
    public boolean getContinue() {
        return true;
    }
}
