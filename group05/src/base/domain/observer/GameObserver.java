package base.domain.observer;

import base.domain.card.Suit;
import base.domain.player.PlayerId;
import base.domain.turn.BidTurn;
import base.domain.turn.PlayTurn;

import java.util.List;

/**
 * Observer interface for entities that want to track the state of the game.
 */
public interface GameObserver {
    default void onBidPlaced(BidTurn bidTurn) {}
    default void onTrumpDetermined(Suit trumpSuit) {}
    default void onTurnPlayed(PlayTurn playTurn) {}
    default void onRoundStarted(List<PlayerId> players) {}
}
