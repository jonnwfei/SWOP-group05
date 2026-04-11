package base.domain.trick;

import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.card.Card;
import base.domain.turn.PlayTurn;

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
    private Player WinningPlayer;
    private Card currentWinningCard;
    private final List<PlayTurn> PlayTurns;

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
        this.WinningPlayer = null;
        this.currentWinningCard = null;
        this.PlayTurns = new ArrayList<>();
    }

    /**
     * Identifies the suit of the first card played in this trick.
     * @return The leading suit, or null if no cards have been played.
     */
    public Suit getLeadingSuit() {
        if (PlayTurns.isEmpty()) return null;
        return PlayTurns.getFirst().playedCard().suit();
    }

    /** @return The player who started the trick. */
    public Player getStartingPlayer() {
        return this.startingPlayer;
    }

    /** @return The player who won the trick or is currently winning the trick if trick isn't finished yet. */
    public Player getWinningPlayer() {
        return this.WinningPlayer;
    }

    /** @return A shallow, immutable copy of the turns played so far. */
    public List<PlayTurn> getTurns() {
        return List.copyOf(this.PlayTurns);
    }

    /** @return true if the trick contains the maximum number of turns. */
    public boolean isCompleted() {
        return this.PlayTurns.size() >= MAX_TURNS;
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
        if (PlayTurns.stream().anyMatch(t -> t.player().equals(player))) {
            throw new IllegalArgumentException("Trick: Player already played in this trick.");
        }

        // Rule: Must follow leading suit if possible
        if (!PlayTurns.isEmpty()) {
            Suit leadingSuit = getLeadingSuit();
            if (playedCard.suit() != leadingSuit && player.hasSuit(leadingSuit)) {
                throw new IllegalArgumentException("Trick: You must follow the leading suit (" + leadingSuit + ").");
            }
        }

        if (PlayTurns.size() >= MAX_TURNS) {
            throw new IllegalArgumentException("Trick: This trick is already full.");
        }

        PlayTurns.add(new PlayTurn(player.getId(), playedCard));
        player.removeCard(playedCard);

        determineWinner(player, playedCard);
    }

    /**
     * Updates the state of the trick with the new winning player and card.
     */
    private void determineWinner(Player player, Card playedCard) {
        if (this.WinningPlayer == null) {
            this.WinningPlayer = player;
            this.currentWinningCard = playedCard;
            return;
        }

        if (beatsCurrentWinner(playedCard)) {
            this.WinningPlayer = player;
            this.currentWinningCard = playedCard;
        }
    }

    /**
     * Evaluates if the played card beats this trick's current winning card.
     */
    private boolean beatsCurrentWinner(Card playedCard) {
        boolean isPlayedCardTrump = (this.trumpSuit != null && playedCard.suit() == this.trumpSuit);
        boolean isBestCardTrump = (this.trumpSuit != null && this.currentWinningCard.suit() == this.trumpSuit);

        if (isPlayedCardTrump) {
            // Trump always beats non-trump; highest trump beats lower trump
            return !isBestCardTrump || playedCard.rank().compareTo(this.currentWinningCard.rank()) > 0;
        }
        else if (!isBestCardTrump && playedCard.suit() == getLeadingSuit()) {
            // If no trump is involved, highest rank of the leading suit wins
            return playedCard.rank().compareTo(this.currentWinningCard.rank()) > 0;
        }
        // suit of played card is not trump suit nor leading suit
        return false;
    }
}