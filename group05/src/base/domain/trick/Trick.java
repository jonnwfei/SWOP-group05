package base.domain.trick;

import base.domain.card.Suit;
import base.domain.card.Card;
import base.domain.player.PlayerId;
import base.domain.turn.PlayTurn;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single trick in a Whist round.
 * Acts as a passive ledger that records turns and calculates the current winning card.
 * @author John Cai
 * @since 25/02/2026
 */
public class Trick {
    /**
     * The maximum number of players/cards in a single trick.
     */
    public static final int MAX_TURNS = 4;

    private final Suit trumpSuit;
    private final PlayerId startingPlayerId;

    private PlayerId winningPlayerId;
    private Card currentWinningCard;
    private final List<PlayTurn> playTurns;

    /**
     * Initializes a new trick.
     * @param startingPlayerId The ID of the player who leads the trick.
     * @param trumpSuit The trump suit for the current round (can be null for Miserie).
     * @throws IllegalArgumentException if startingPlayerId is null.
     */
    public Trick(PlayerId startingPlayerId, Suit trumpSuit) {
        if (startingPlayerId == null) {
            throw new IllegalArgumentException("Trick: Starting player ID must exist.");
        }

        this.trumpSuit = trumpSuit;
        this.startingPlayerId = startingPlayerId;
        this.winningPlayerId = null;
        this.currentWinningCard = null;
        this.playTurns = new ArrayList<>();
    }

    /**
     * Identifies the suit of the first card played in this trick.
     *
     * @return The leading suit, or null if no cards have been played.
     */
    public Suit getLeadingSuit() {
        if (playTurns.isEmpty())
            return null;
        return playTurns.getFirst().playedCard().suit();
    }

    /** @return The ID of the player who won the trick or is currently winning. */
    public PlayerId getWinningPlayerId() {
        return this.winningPlayerId;
    }

    /** @return The ID of the player who started the trick. */
    public PlayerId getStartingPlayerId() {
        return this.startingPlayerId;
    }

    /** @return A shallow, immutable copy of the turns played so far. */
    public List<PlayTurn> getTurns() {
        return List.copyOf(this.playTurns);
    }

    /** @return true if the trick contains the maximum number of turns. */
    public boolean isCompleted() {
        return this.playTurns.size() >= MAX_TURNS;
    }

    /**
     * Records a player's move in the Trick and recalculates the winning player.
     * Note: Rule validation (e.g., "Must follow suit") and hand mutation MUST be
     * handled by the Active Coordinator (PlayState) before calling this method.
     * @param playerId The ID of the player making the move.
     * @param playedCard The card being played.
     * @throws IllegalArgumentException if the player already played in this trick.
     * @throws IllegalStateException if the trick is already full.
     */
    public void addTurn(PlayerId playerId, Card playedCard) {
        if (playerId == null || playedCard == null) {
            throw new IllegalArgumentException("Trick: PlayerId and Card must exist.");
        }
        if (isCompleted()) {
            throw new IllegalStateException("Trick: This trick is already full.");
        }

        // Rule: One card per player
        if (playTurns.stream().anyMatch(t -> t.playerId().equals(playerId))) {
            throw new IllegalArgumentException("Trick: Player has already played in this trick.");
        }

        playTurns.add(new PlayTurn(playerId, playedCard));
        updateWinningPlayer(playerId, playedCard);
    }

    /**
     * Updates the state of the trick using the globally defined TrickEvaluator rules.
     */
    private void updateWinningPlayer(PlayerId playerId, Card playedCard) {
        if (this.winningPlayerId == null) {
            this.winningPlayerId = playerId;
            this.currentWinningCard = playedCard;
            return;
        }

        // Delegate the complex math to our Pure Fabrication evaluator!
        TrickEvaluator evaluator = new TrickEvaluator(getLeadingSuit(), this.trumpSuit);

        if (evaluator.doesBeat(playedCard, this.currentWinningCard)) {
            this.winningPlayerId = playerId;
            this.currentWinningCard = playedCard;
        }
    }
}