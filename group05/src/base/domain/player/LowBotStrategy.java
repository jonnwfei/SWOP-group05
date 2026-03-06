package base.domain.player;

import base.domain.bid.Bid;
import base.domain.bid.PassBid;
import base.domain.card.Card;
import base.domain.card.Suit;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A basic Bot strategy for a simulated player.
 * <p>
 * During the Bidding Phase, it will automatically pass.
 * During the Play Phase, it will always evaluate its legal options
 * and play the lowest-ranking card available to it.
 *
 * @author Tommy Wu
 * @since 25/02/2026
 */
public class LowBotStrategy implements Strategy {

    @Override
    public Bid determineBid(Player player) {return new PassBid(player); // PASS is BidType
    }

    @Override
    public Card chooseCardToPlay(List<Card> currentHand, Suit lead) {
        List<Card> legalCards = determineLegalCards(currentHand, lead);
        return Collections.min(legalCards, Comparator.comparing(Card::getRank));
    }

    private List<Card> determineLegalCards(List<Card> currentHand, Suit lead) {
        if (currentHand == null) throw new IllegalArgumentException("currentHand can't be null");
        if (currentHand.isEmpty()) throw new IllegalArgumentException("currentHand can't be empty");

        List<Card> legalCards = currentHand.stream().filter(card -> card.getSuit() == lead).toList();
        if (legalCards.isEmpty()) {legalCards = currentHand;}
        return legalCards;
    }

    @Override
    public boolean requiresConfirmation() {
        return false;
    }
}