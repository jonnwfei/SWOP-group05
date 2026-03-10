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
        String currentPlayerName,
        int trickNumber,
        List<Card> cardsOnTable,
        List<Card> currentPlayerHand,
        List<Card> exposedHand // List of Cards instead of a formatted String
) implements GameEvent {
    public PickCardEvent {
        cardsOnTable = List.copyOf(cardsOnTable);
        currentPlayerHand = List.copyOf(currentPlayerHand);
        // exposedHand can be null if not an Open Miserie
        if (exposedHand != null) exposedHand = List.copyOf(exposedHand);
    }
}