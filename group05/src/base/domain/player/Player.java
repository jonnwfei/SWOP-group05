package base.domain.player;

import base.domain.bid.Bid;
import base.domain.card.Card;
import base.domain.card.Suit;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a participant in the trick-taking card game.
 * <p>
 * This class encapsulates the player's state, such as their hand and score. It utilizes
 * the Strategy design pattern ({@link Strategy}) to decouple the entity from its decision-making
 * logic, allowing the same {@code Player} class to be used for both human players and AI opponents.
 *
 * @author Tommy Wu
 * @since 24/02/2026
 */
public class Player {
    private final Strategy decisionStrategy;
    private final String name;
    private List<Card> currentHand;
    private Integer playerScore;

    /**
     * Constructs a new player with a specific decision-making strategy and name.
     *
     * @param decisionStrategy the behavior strategy used to make bids and play cards.
     * @param name             the display name of the player.
     * @throws IllegalArgumentException if {@code decisionStrategy} or {@code name} is null.
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
     * @param suit to check
     * @throws IllegalArgumentException | suit == null
     * @return Boolean
     */
    public Boolean hasSuit(Suit suit) {
        if (suit == null) throw new IllegalArgumentException("suit can't be null");
        return currentHand.stream().anyMatch(card -> card.suit() == suit);
    }

    /**
     * Adds a dealt card to the player's current hand.
     *
     * @param card the {@link Card} to add to the hand.
     * @throws IllegalArgumentException if the {@code card} is null.
     * @throws IllegalStateException    if the player's hand already contains the maximum of 13 cards.
     */
    public void addCard(Card card) {
        if (card == null) throw new IllegalArgumentException("card can't be null");
        if (this.currentHand.size() >= 13) throw new IllegalStateException("Player hand is full");
        this.currentHand.add(card);
    }

    /**
     * Empties the player's hand, typically called at the end of a round before redealing.
     */
    public void flushHand() {
        this.currentHand = new ArrayList<>();
    }

    /**
     * Prompts the player's strategy to select a card to play based on the current trick's lead suit.
     * <p>
     * <b>Game Rules Enforced:</b> A player must follow the lead suit if they possess it.
     * If they are leading the trick ({@code lead} is null), they may play any card in their hand.
     *
     * @param lead the leading {@link Suit} of the current trick, or {@code null} if this player is leading the trick.
     * @return the {@link Card} chosen by the player's strategy.
     */
    public Card chooseCard(Suit lead) {
        return this.decisionStrategy.chooseCardToPlay(this.getHand(), lead);
    }

    /**
     * Removes a specific card from the player's hand after it has been played.
     *
     * @param card the {@link Card} to remove.
     * @throws IllegalArgumentException if the {@code card} is null, or if it is not present in the hand.
     */
    public void removeCard(Card card) {
        if (card == null) throw new IllegalArgumentException("card can't be null.");
        if (!this.currentHand.remove(card)) { throw new IllegalArgumentException("domain.card.Card is not in player hand."); }
    }

    /**
     * Prompts the player's strategy to determine their bid for the current round.
     *
     * @return the {@link Bid} chosen by the player's strategy.
     */
    public Bid chooseBid() {return this.decisionStrategy.determineBid(this);}

    /**
     * updates the player score.
     * @param score the score to be added or deducted to the current player score.
     */
    public void updateScore(int score) {this.playerScore = this.playerScore + score;}


    /**
     * Retrieves a defensive copy of the player's current hand to prevent external modification.
     *
     * @return a new {@code List} containing the player's current {@link Card}s.
     */
    public List<Card> getHand() {
        return new ArrayList<>(this.currentHand);
    }

    /**
     * Retrieves the player's current cumulative score.
     *
     * @return the player's score.
     */
    public Integer getScore() {return this.playerScore;}

    /**
     * Retrieves the display name of the player.
     *
     * @return the player's name.
     */
    public String getName() {return this.name;}
}
