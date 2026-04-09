package base.domain.observer;

import base.domain.bid.Bid;
import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.trick.Turn;

import java.util.List;

/**
 * Observer interface for entities that want to track the state of the game.
 */
public interface GameObserver {
    default void onBidPlaced(Bid bid) {}
    default void onTrumpDetermined(Suit trumpSuit) {}
    default void onTurnPlayed(Turn turn) {}
    default void onRoundStarted(List<Player> players) {}
}
