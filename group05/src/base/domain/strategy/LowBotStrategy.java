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
     * @return A passBid instance.
     */
    @Override
    public Bid determineBid(List<Card> hand) {
        return new PassBid();
    }

    /**
     * Selects the card with the lowest rank from the set of legal moves.
     *
     * @param currentHand The list of cards currently held by the player.
     *@param lead        The suit led in the current trick (maybe null if the bot leads).
     * @return The card with the minimum rank among legal choices.
     */
    @Override
    public Card chooseCardToPlay(List<Card> currentHand, Suit lead) {
        if (currentHand == null || currentHand.isEmpty()) {
            throw new IllegalArgumentException("Cannot choose a card from an empty or null hand.");
        }
        List<Card> legalCards = CardMath.getLegalCards(currentHand, lead);
        if (legalCards.isEmpty()) {
            throw new IllegalStateException("Critical Error: Legal cards filtered to empty list.");
        }
        return Collections.min(legalCards, Comparator.comparing(Card::rank));
    }

}