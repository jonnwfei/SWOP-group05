package cli.events.CountEvents;

import base.domain.player.Player;
import cli.events.IOEvent;

import java.util.List;

public record PlayerSelectionIOEvent(List<Player> players, boolean multi) implements IOEvent {
    public boolean needsInput() { return true; }
    public boolean getContinue() { return true; }
}
