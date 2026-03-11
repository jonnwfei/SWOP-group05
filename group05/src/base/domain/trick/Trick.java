package base.domain.trick;

import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.card.Card;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single trick in a Whist round
 * @author John Cai
 * @since 25/02/2026
 */
public class Trick {
    /** The maximum number of players/cards in a single trick. */
    public static final int MAX_TURNS = 4;

    private final Suit trumpSuit;
    private final Player startingPlayer;
    private Player winningPlayer;
    private final List<Turn> turns;

    /**
     * Initializes a new trick.
     * @param startingPlayer The player who leads the trick.
     * @param trumpSuit The trump suit for the current round (can be null).
     * @throws IllegalArgumentException if startingPlayer is null.
     */
    public Trick(Player startingPlayer, Suit trumpSuit) {
        if (startingPlayer == null)
            throw new IllegalArgumentException("Trick: Starting player must exist.");

        this.trumpSuit = trumpSuit;
        this.startingPlayer = startingPlayer;
        this.winningPlayer = null;
        this.turns = new ArrayList<>();
    }

    /**
     * Identifies the suit of the first card played in this trick.
     * @return The leading suit, or null if no cards have been played.
     */
    public Suit getLeadingSuit() {
        if (turns.isEmpty()) return null;
        return turns.get(0).playedCard().suit();
    }

    /** @return The player who started the trick. */
    public Player getStartingPlayer() {
        return this.startingPlayer;
    }

    /** @return The player who won the trick (null until 4 cards are played). */
    public Player getWinningPlayer() {
        return this.winningPlayer;
    }

    /** @return A shallow, immutable copy of the turns played so far. */
    public List<Turn> getTurns() {
        return List.copyOf(this.turns);
    }

    /** @return true if the trick contains the maximum number of turns. */
    public boolean isCompleted() {
        return this.turns.size() >= MAX_TURNS;
    }

    /**
     * Processes a player's move, validates rules, and removes the card from their hand.
     * @param player The player attempting to play.
     * @param playedCard The card to be played.
     * @throws IllegalArgumentException if the player already played, the move is illegal
     * (not following suit), or the trick is full.
     */
    public void playCard(Player player, Card playedCard) {
        if (player == null || playedCard == null)
            throw new IllegalArgumentException("Trick: Player and Card must exist.");

        // Rule: One card per player
        if (turns.stream().anyMatch(t -> t.player().equals(player))) {
            throw new IllegalArgumentException("Trick: Player already played in this trick.");
        }

        // Rule: Must follow leading suit if possible
        if (!turns.isEmpty()) {
            Suit leadingSuit = getLeadingSuit();
            if (playedCard.suit() != leadingSuit && player.hasSuit(leadingSuit)) {
                throw new IllegalArgumentException("Trick: You must follow the leading suit (" + leadingSuit + ").");
            }
        }

        if (turns.size() >= MAX_TURNS) {
            throw new IllegalArgumentException("Trick: This trick is already full.");
        }

        turns.add(new Turn(player, playedCard));
        player.removeCard(playedCard);

        if (turns.size() == MAX_TURNS) {
            determineWinner();
        }
    }

    /**
     * Evaluates all turns based on Trump priority and Rank to identify the winner.
     */
    private void determineWinner() {
        Suit leadingSuit = getLeadingSuit();
        Player currentWinner = null;
        Card bestCard = null;

        for (Turn turn : turns) {
            Player player = turn.player();
            Card playedCard = turn.playedCard();

            if (bestCard == null) {
                currentWinner = player;
                bestCard = playedCard;
                continue;
            }

            boolean isNewCardTrump = (this.trumpSuit != null && playedCard.suit() == this.trumpSuit);
            boolean isBestCardTrump = (this.trumpSuit != null && bestCard.suit() == this.trumpSuit);

            if (isNewCardTrump) {
                // Trump always beats non-trump; highest trump beats lower trump
                if (!isBestCardTrump || playedCard.rank().compareTo(bestCard.rank()) > 0) {
                    currentWinner = player;
                    bestCard = playedCard;
                }
            } else if (!isBestCardTrump) {
                // If no trump is involved, highest rank of the leading suit wins
                if (playedCard.suit() == leadingSuit && playedCard.rank().compareTo(bestCard.rank()) > 0) {
                    currentWinner = player;
                    bestCard = playedCard;
                }
            }
        }
        this.winningPlayer = currentWinner;
    }
}