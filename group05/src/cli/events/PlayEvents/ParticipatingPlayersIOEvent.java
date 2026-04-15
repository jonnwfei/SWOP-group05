package cli.events.PlayEvents;

import base.domain.results.ParticipatingPlayersResult;
import cli.events.IOEvent;

public record ParticipatingPlayersIOEvent(ParticipatingPlayersResult data) implements IOEvent {
    public boolean needsInput() { return true; }
    public boolean getContinue() { return true; }
}
