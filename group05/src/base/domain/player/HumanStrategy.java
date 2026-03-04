package base.domain.player;

import base.domain.bid.Bid;
import base.domain.card.Card;

import java.util.List;

/**
 * @author Tommy Wu
 * @since 25/02/2026
 */
public class HumanStrategy implements Strategy {
    @Override
    public Bid determineBid() {
        return null;
    }

    @Override
    public Card chooseCardToPlay(List<Card> legalCards) {
        return null;
    }

    @Override
    public Boolean requiresConfirmation() {
        return true;
    }
}
