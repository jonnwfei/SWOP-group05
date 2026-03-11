package base.domain.events.bidevents;

import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.events.GameEvent;

import java.util.List;

public record BidTurnEvent(String playerName, Suit dealtTrump, BidType currentHighestBidType, BidType[] bidTypes, List<Card> playerHand) implements GameEvent<Integer> {
    @Override
    public Class<Integer> getInputType() {
        return Integer.class;
    }

    @Override
    public boolean isValid(Integer input) {
        return input >= 1 && input <= bidTypes.length;
    }
    @Override
    public boolean needsInput(){
        return true;
    }

}
