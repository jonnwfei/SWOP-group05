package cli.events.CountEvents;

import cli.events.IOEvent;

import java.util.List;

public record ScoreBoardIOEvent(List<String> playerNames, List<Integer> scores, boolean canRemovePlayer) implements IOEvent {
    public boolean needsInput() { return true; }
    public boolean getContinue() { return true; }
}
