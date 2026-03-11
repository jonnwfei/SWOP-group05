package base.domain.events.bidevents;

import base.domain.player.Player;
import base.domain.events.GameEvent;

public record RejectedProposalEvent(String proposerName) implements GameEvent<Integer> {
    @Override
    public Class<Integer> getInputType() {
        return Integer.class;
    }

    @Override
    public boolean isValid(Integer input) {
        if (input == 1 || input == 2) {
            return true;
        }
        else{
            return false;
        }
    }
    @Override
    public boolean needsInput(){
        return true;
    }
}
