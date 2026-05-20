package base.domain.deck;

import base.domain.WhistRules;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a standard 52-card playing deck.
 */
public class Deck {

    private final List<Card> cards;

    /**
     * Constructs a new Deck, populating it with all 52 combinations
     * of Suit and Rank, and immediately shuffles it.
     */
    public Deck() {
        this.cards = new ArrayList<>();
        initializeDeck();
    }

    /**
     * Creates the deck and shuffles it
     */
    private void initializeDeck() {
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }
        Collections.shuffle(cards);
    }

    /**
     * Distributes the entire 52-card deck to 4 players.
     * Adheres to dealing rules: batches of 4,
     * then another batch of 4, and a final batch of 5 cards per player.
     *
     * @return A list containing 4 hands, each represented as a List of 13 cards.
     */
    public List<List<Card>> deal() {
        List<List<Card>> hands = new ArrayList<>();
        for (int i = 0; i < WhistRules.REQUIRED_PLAYERS; i++) {
            hands.add(new ArrayList<>());
        }

        int currentCardIndex = 0;
        int[] dealRounds = {4, 4, 5};

        // Deal in rounds of 4, then another 4, then 5 cards per player
        for (int cardsToGive : dealRounds) {
            for (int playerIndex = 0; playerIndex < WhistRules.REQUIRED_PLAYERS; playerIndex++) {
                for (int i = 0; i < cardsToGive; i++) {
                    hands.get(playerIndex).add(cards.get(currentCardIndex++));
                }
            }
        }
        return hands;
    }

    /**
     * @return A shallow copy of the internal cards list.
     */
    public List<Card> getCards() {
        return new ArrayList<>(cards);
    }

    /**
     * Randomizes the order of the cards currently in the deck.
     */
    public void shuffle(){
        Collections.shuffle(cards);
    }
}