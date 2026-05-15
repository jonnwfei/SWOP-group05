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
 * An aggressive Bot strategy for a simulated player that attempts to win tricks.
 *
 * @author Tommy Wu
 * @since 25/02/2026
 */
public final class HighBotStrategy implements Strategy {

    /**
     * Always returns a PassBid. Despite high play-play aggression, this bot
     * does not currently participate in bidding.
     * @return A {@link PassBid} instance.
     */
    @Override
    public Bid determineBid(List<Card> hand) {
        return new PassBid();
    }

    /**
     * Selects the card with the highest rank from the set of legal moves.
     *
     * @param currentHand The list of cards currently held by the player.
     * @param lead        The suit led in the current trick
     * @return The card with the maximum rank among legal choices.
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
        return Collections.max(legalCards, Comparator.comparing(Card::rank));
    }
}