package cli.events.menu;

import base.domain.round.Round;
import cli.events.IOEvent;

import java.util.List;

public record DeleteRoundIOEvent(List<Round> rounds) implements IOEvent {
    @Override
    public boolean needsInput() {
        return true;
    }

    @Override
    public boolean getContinue() {
        return true;
    }
}
