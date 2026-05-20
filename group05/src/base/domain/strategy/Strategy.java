package base.domain.strategy;

import base.domain.bid.Bid;
import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.observer.GameEventPublisher;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.snapshots.StrategySnapshotType;

import java.util.List;

/**
 * Defines the decision-making behavior for a player in the game.
 * <p>
 * This interface implements the Strategy design pattern, completely decoupling the
 * {@link Player} entity from the logic used to make game decisions.
 * This allows human players (driven by UI/IO input) and AI bots (driven by algorithms)
 * to be treated identically by the core game engine.
 *
 * @author Tommy Wu
 * @since 24/02/2026
 */
public sealed interface Strategy permits HighBotStrategy, HumanStrategy, LowBotStrategy, SmartBotStrategy{

    /**
     * Determines the contract bid the player wishes to make during the Bidding Phase.
     * <p>
     * For a human strategy, this will trigger a prompt to the UI/Console.
     * For an AI strategy, this will algorithmically evaluate the player's hand to calculate the optimal bid. (currently just PASS)
     *
     * @return the chosen {@link Bid}.
     */
    Bid determineBid(List<Card> hand);


    Card chooseCardToPlay(List<Card> currentHand, Suit lead );

    StrategySnapshotType toSnapshotType() ;

    static Strategy toStrategy(StrategySnapshotType snapshotType, PlayerId restoredId) {
        return switch (snapshotType) {
            case HIGH_BOT -> new HighBotStrategy();
            case LOW_BOT -> new LowBotStrategy();
            case SMART_BOT -> new SmartBotStrategy(restoredId); //TODO: still code smell, wait for refactor of tommy
            case HUMAN -> new HumanStrategy();
        };
    }

    /**
     * Lifecycle hook called when the strategy is added to a live game.
     * @param publisher A restricted interface to subscribe to game events.
     */
    default void onJoinGame(GameEventPublisher publisher) {}
}
