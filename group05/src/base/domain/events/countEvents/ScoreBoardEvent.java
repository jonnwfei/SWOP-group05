package base.domain.events.countEvents;

import base.domain.events.GameEvent;
import base.domain.player.Player;

import java.util.List;

public record ScoreBoardEvent(List<String> playerNames, List<Integer> scores) implements GameEvent<Integer> {
    @Override
    public Class<Integer> getInputType() {
        return Integer.class;
    }

    @Override
    public boolean isValid(Integer input) {
        if (input < 1 || input > 2) {
            return false;
        }
        else{
            return true;
        }
    }
    @Override
    public boolean needsInput(){
        return true;
    }
}