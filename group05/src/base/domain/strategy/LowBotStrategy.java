package base.domain.strategy;

import base.domain.bid.Bid;
import base.domain.bid.PassBid;
import base.domain.card.Card;
import base.domain.card.CardMath;
import base.domain.card.Suit;
import base.domain.player.PlayerId;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A basic Bot strategy for a simulated player that prioritizes preserving high-ranking cards.
 *
 * @author Tommy Wu
 * @since 25/02/2026
 */
public final class LowBotStrategy implements Strategy {

    /**
     * Always returns a PassBid. This strategy does not attempt to win bids.
     * @param playerId The player instance using this strategy.
     * @return A passBid instance.
     */
    @Override
    public Bid determineBid(PlayerId playerId, List<Card> hand) {
        return new PassBid(playerId);
    }

    /**
     * Selects the card with the lowest rank from the set of legal moves.
     *
     * @param playerId
     * @param currentHand The list of cards currently held by the player.
     *@param lead        The suit led in the current trick (maybe null if the bot leads).
     * @return The card with the minimum rank among legal choices.
     */
    @Override
    public Card chooseCardToPlay(PlayerId playerId, List<Card> currentHand, Suit lead) {
        List<Card> legalCards = CardMath.getLegalCards(currentHand, lead);
        return Collections.min(legalCards, Comparator.comparing(Card::rank));
    }

}