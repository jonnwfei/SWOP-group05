//@author: Tommy Wu
//@since: 24/02/2026
import Card;

import java.util.List;

interface Strategy {

    public Bid determineBid();
        // player filters legal cards
    public Card chooseCardToPlay(List<Card> legalCards);
}
