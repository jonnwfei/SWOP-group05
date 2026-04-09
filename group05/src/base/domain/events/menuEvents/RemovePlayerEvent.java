package base.domain.events.menuEvents;

import base.domain.events.GameEvent;
import base.domain.player.Player;

import java.util.List;

public record RemovePlayerEvent(List<Player> players) implements GameEvent<Integer> {
    @Override
    public Class<Integer> getInputType() {
        return Integer.class;
    }

    @Override
    public boolean isValid(Integer input) {
        return input >= 0 && input < players.size();
    }

    @Override
    public boolean needsInput() {
        return true;
    }
}