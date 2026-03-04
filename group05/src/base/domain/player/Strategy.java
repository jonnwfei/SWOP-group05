package base.domain.player;

import base.domain.bid.Bid;
import base.domain.card.Card;

import java.util.List;

/**
 * @author Tommy Wu
 * @since 24/02/2026
 */
public interface Strategy {
    Bid determineBid();
    Card chooseCardToPlay(List<Card> legalCards);
    Boolean requiresConfirmation();
}
