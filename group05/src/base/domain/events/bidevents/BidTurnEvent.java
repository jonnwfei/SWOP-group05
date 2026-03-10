package base.domain.events.bidevents;

import base.domain.bid.BidType;
import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.events.GameEvent;

public record BidTurnEvent(String playerName, Suit dealtTrump, BidType currentHighestBidType, BidType[] bidTypes) implements GameEvent<Integer> {
    @Override
    public Class<Integer> getInputType() {
        return Integer.class;
    }

    @Override
    public boolean isValid(Integer input) {
        if (input < 1 || input > 16) {
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
