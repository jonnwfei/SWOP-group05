package base.domain.player;

import base.domain.bid.Bid;
import base.domain.bid.PassBid;
import base.domain.card.Card;

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
    public Card chooseCardToPlay(List<Card> legalCards) {
        if (legalCards == null) throw new IllegalArgumentException("legalCards can't be null");
        if (legalCards.isEmpty()) throw new IllegalArgumentException("legalCards can't be empty");
        return Collections.min(legalCards, Comparator.comparing(Card::rank));
    }

    @Override
    public boolean requiresConfirmation() {
        return false;
    }
}