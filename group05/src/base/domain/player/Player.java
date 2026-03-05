package base.domain.player;
import base.domain.bid.Bid;
import base.domain.card.Suit;
import base.domain.card.Card;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Tommy Wu
 * @since 24/02/2026
 */
public class Player {
    private final Strategy decisionStrategy;
    private final String name;
    private List<Card> currentHand;
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
        if (this.currentHand.size() >= 13) throw new IllegalStateException("Player hand is full");
        this.currentHand.add(card);
    }

    /**
     * empties Hand of player
     */
    public void flushHand() {
        this.currentHand = new ArrayList<>();
    }

    /**
     * Player
     * @param lead | current lead suit of the trick
     * @return Card chosen by the player following its strategy
     */
    public Card playCard(Suit lead) {
        // If we are leading the trick, we can play any card!
        if (lead == null) {
            Card chosenCard = this.decisionStrategy.chooseCardToPlay(this.getHand());
            this.removeCard(chosenCard);
            return chosenCard;
        }
        // Otherwise, we must follow suit if possible.
        List<Card> legalCards = this.getHand().stream().filter(card -> card.suit() == lead).toList();
        if (legalCards.isEmpty()) { legalCards = this.getHand(); }
        Card chosenCard = this.decisionStrategy.chooseCardToPlay(legalCards);
        this.removeCard(chosenCard);
        return chosenCard;
}

    public Bid chooseBid() {return this.decisionStrategy.determineBid();}

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
    public Integer getScore() {return this.playerScore;}

    public String getName() {return this.name;}
}
