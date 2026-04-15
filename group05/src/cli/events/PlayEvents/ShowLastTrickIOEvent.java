package cli.events.PlayEvents;

import base.domain.trick.Trick;
import cli.events.IOEvent;

public record ShowLastTrickIOEvent(Trick lastTrick) implements IOEvent {
    @Override
    public boolean needsInput() {
        return false;
    }

    @Override
    public boolean getContinue() {
        return false;
    }
}
