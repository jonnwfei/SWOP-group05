package base.domain.observer;

import base.domain.bid.Bid;
import base.domain.card.Card;
import base.domain.card.Suit;

/**
 * Observer interface for entities that want to track the state of the game.
 */
public interface TableObserver {
    default void onBidPlaced(Bid bid) {}
    default void onTrumpDetermined(Suit trumpSuit) {}
    default void onCardPlayed(Card card) {}
    default void onRoundStarted() {}
}
