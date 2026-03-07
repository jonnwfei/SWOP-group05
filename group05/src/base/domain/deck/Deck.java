package base.domain.deck;

import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {

    private final List<Card> cards;

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
     * Deals the cards to the 4 players
     * @return A list of 4 lists containing the whole deck dealt correctly
     */
    public List<List<Card>> deal() {
        List<List<Card>> hands = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            hands.add(new ArrayList<>());
        }

        int currentCardIndex = 0;
        int[] dealRounds = {4, 4, 5};

        // Deel in rondes van 4, dan weer 4, dan 5 kaarten per speler
        for (int cardsToGive : dealRounds) {
            for (int playerIndex = 0; playerIndex < 4; playerIndex++) {
                for (int i = 0; i < cardsToGive; i++) {
                    hands.get(playerIndex).add(cards.get(currentCardIndex++));
                }
            }
        }
        return hands;
    }
    public List<Card> getCards() {
        return new ArrayList<>(cards);
    }
}