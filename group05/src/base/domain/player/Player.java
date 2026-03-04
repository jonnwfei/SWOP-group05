package base.domain.player;

import base.domain.card.Card;
import base.domain.card.Suit;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tommy Wu
 * @since 24/02/2026
 */
public class Player {
    private final Strategy decisionStrategy;
    private final String name;
    private final List<Card> currentHand;
    private Integer playerScore;

    /**
     * @param decisionStrategy
     * @param name
     * @throws IllegalArgumentException | decisionStrategy == null || name == null
     */
    public Player(Strategy decisionStrategy, String name) {
        if (decisionStrategy == null || name == null) throw new IllegalArgumentException("Strategy and name can't be null");
        this.decisionStrategy = decisionStrategy;
        this.name = name;
        this.currentHand = new ArrayList<>();
        this.playerScore = 0;
    }

    /**
     * returns True if Player has given suit, false otherwise.
     * @param suit
     * @throws IllegalArgumentException | suit == null
     * @return Boolean
     */
    public Boolean hasSuit(Suit suit) {
        if (suit == null) throw new IllegalArgumentException("suit can't be null");
        return currentHand.stream().anyMatch(card -> card.getSuit() == suit);
    }

    /**
     * adds card to player hand
     * @throws IllegalArgumentException | card == null
     * @throws IllegalStateException | this.getHand().size() >= 13
     * @param card
     */
    public void addCard(Card card) {
        if (card == null) throw new IllegalArgumentException("card can't be null");
        if (currentHand.size() >= 13) throw new IllegalStateException("Player hand is full");
        currentHand.add(card);
    }

    /**
     * @throws IllegalArgumentException | card == null
     * @throws IllegalStateException | this.getHand().isEmpty()
     * @throws IllegalArgumentException | !this.getHand().contains(card)
     * @param card
     */
    private void removeCard(Card card) {
        if (card == null) throw new IllegalArgumentException("card can't be null.");
        if (!currentHand.remove(card)) { throw new IllegalArgumentException("domain.card.Card is not in player hand."); }
    }

    /**
     * gives a duplicate list of currentHand
     */
    public List<Card> getHand() {
        return new ArrayList<>(currentHand);
    }

    /**
     * returns player score
     */
    public Integer getScore() {
        return this.playerScore;
    }
}
