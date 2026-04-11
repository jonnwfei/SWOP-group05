package base.domain.strategy;

import base.domain.bid.Bid;
import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.player.Player;

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
     * @param player the player making the bid, required to properly construct and bind the resulting {@link Bid} contract.
     * @return the chosen {@link Bid}.
     */
    Bid determineBid(Player player);


    Card chooseCardToPlay(List<Card> currentHand, Suit lead );

    /**
     * Indicates whether this strategy requires a manual confirmation
     * before the game engine proceeds to the next turn.
     * <p>
     * @return {@code true} if the game loop should pause/wait for confirmation, {@code false} otherwise.
     */
    boolean requiresConfirmation();
}
