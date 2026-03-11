package base.domain.events.playevents;

import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.player.Player;
import base.domain.round.Round;
import base.domain.trick.Turn;
import base.domain.events.GameEvent;

import java.util.List;

public record PickCardEvent(
        List<Card> cardsOnTable,      // e.g., ["Player1 played ACE of SPADES", ...]
        boolean isOpenMiserie,         // Tells the UI whether to draw the exposed hand
        String exposedPlayerName,      // Who is playing Open Miserie
        List<Card> formattedExposedHand,   // The actual cards to show everyone
        int trickNumber,
        String currentPlayerName,
        List<Card> currentPlayerHand // Formatted strings of the cards they can play
) implements GameEvent<Integer> {
    @Override
    public Class<Integer> getInputType() {
        return Integer.class;
    }

    @Override
    public boolean isValid(Integer input) {
        return input >= 0 && input <= currentPlayerHand.size();
    }
    @Override
    public boolean needsInput(){
        return true;
    }
}
