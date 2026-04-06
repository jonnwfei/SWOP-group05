package base.domain.player;

import base.domain.bid.Bid;
import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.deck.Deck;

import java.util.List;

public class SmartBotStrategy implements Strategy {

    private enum behavior {
        MISERIE,
        ANTI_MISERIE,
        NORMAL
    }

    private behavior currentBehavior = behavior.NORMAL;
    private final List<Card> unplayedCards = new Deck().getCards();

    @Override
    public Bid determineBid(Player player) {
        return null;
    }

    private int estimateWinningTricks(List<Card> hand) {
        return 0;
    }

    @Override
    public Card chooseCardToPlay(List<Card> currentHand, Suit lead) {
        return switch (this.currentBehavior) {
            case MISERIE       -> playMiserieLogic(currentHand, lead);
            case ANTI_MISERIE  -> playAntiMiserieLogic(currentHand, lead);
            case NORMAL        -> playNormalLogic(currentHand, lead);
        };
    }

    private Card playMiserieLogic(List<Card> currentHand, Suit lead) {
        return null;
    }

    private Card playAntiMiserieLogic(List<Card> currentHand, Suit lead) {
        return null;
    }

    private Card playNormalLogic(List<Card> currentHand, Suit lead) {
        return null;
    }

    @Override
    public boolean requiresConfirmation() {
        return false;
    }




}
