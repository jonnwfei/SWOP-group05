package base.domain.player;

import base.domain.bid.Bid;
import base.domain.card.Card;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author Tommy Wu
 * @since 25/02/2026
 */
public class LowBotStrategy implements Strategy {

    @Override
    // always returns pass
    public Bid determineBid(Player player) {
        return new Bid(player, BidType.PASS); // PASS is BidType
    }

    @Override
    public Card chooseCardToPlay(List<Card> legalCards) {
        if (legalCards == null) throw new IllegalArgumentException("legalCards can't be null");
        if (legalCards.isEmpty()) throw new IllegalArgumentException("legalCards can't be empty");
        return Collections.min(legalCards, Comparator.comparing(Card::getRank));
    }

    @Override
    public boolean requiresConfirmation() {
        return false;
    }
}