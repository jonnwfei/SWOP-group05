package base.domain.events.bidevents;

import base.domain.bid.BidType;
import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.events.GameEvent;

public record SuitPromptEvent(String playerName, BidType pendingType, Suit[] suits) implements GameEvent<Integer> {
    @Override
    public Class<Integer> getInputType() {
        return Integer.class;
    }

    @Override
    public boolean isValid(Integer input) {
        if (input < 1 || input > 4) {
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
