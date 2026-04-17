package base.domain.player;

import base.domain.bid.Bid;
import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.strategy.Strategy;

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
    private final String playerName;
    private final PlayerId playerId;
    private final List<Card> currentHand;
    private Integer playerScore;

    /**
     * Constructs a new player with a specific decision-making strategy, name, and unique identifier.
     * <p>
     * This constructor requires an explicit {@link PlayerId} to ensure identity preservation
     * across different game states and persistence layers.
     *
     * @param decisionStrategy the behavior strategy used to make bids and play cards.
     * @param name             the display name of the player.
     * @param playerId         the unique identifier for this player instance.
     * @throws IllegalArgumentException if {@code decisionStrategy}, {@code name}, or {@code playerId} is null.
     */
    public Player(Strategy decisionStrategy, String name, PlayerId playerId) {
        if (decisionStrategy == null || name == null || playerId == null) throw new IllegalArgumentException("Strategy, name and/or Id can't be null");
        this.decisionStrategy = decisionStrategy;
        this.playerName = name;
        this.playerId = playerId;
        this.currentHand = new ArrayList<>();
        this.playerScore = 0;
    }

    /**
     * Sets the given Hand as this player's currentHand,
     * @param hand to set
     * @throws IllegalArgumentException if hand is null
     */
    public void setHand(List<Card> hand) {
        if (hand == null) throw new IllegalArgumentException("The given hand can't be null");

        // First clear then Add the cards to the player's internal hand
        this.currentHand.clear();
        this.currentHand.addAll(hand);

        // sort this hand
        this.currentHand.sort((c1, c2) -> {
            int suitCompare = c1.suit().compareTo(c2.suit());
            if (suitCompare != 0) {
                return suitCompare;
            }
            return c2.rank().compareTo(c1.rank()); // High to low
        });
    }

    /**
     * Empties the player's hand, typically called at the end of a round before redealing.
     */
    public void flushHand() {
        this.currentHand.clear();
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
    public Bid chooseBid() {return this.decisionStrategy.determineBid(playerId, currentHand);}

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
    public String getName() {return this.playerName;}

    /**
     * Retrieves the player's unique Id
     * @return the player's Id.
     */
    public PlayerId getId() {return this.playerId;}

    /**
     * Exposes the strategy type for persistence mapping.
     */
    public Strategy getDecisionStrategy() {
        return this.decisionStrategy;
    }

    /**
     * Retrieves boolean whether player needs confirmation or not.
     *
     * @return true if Player is a player, false if it's a bot
     */
    public boolean getRequiresConfirmation() {
        return this.decisionStrategy.requiresConfirmation();
    }

    /** Checks if the player holds a specific card */
    public boolean hasCard(Card card) {
        if (card == null) throw new IllegalArgumentException("card can't be null.");
        return currentHand.stream().anyMatch(card::equals);
    }
}
