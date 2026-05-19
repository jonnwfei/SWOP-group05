package base.domain.trick;

import base.domain.card.Card;
import base.domain.card.Suit;

/**
 * Encapsulates the rules for a specific trick.
 * Evaluates which cards win based on the active lead and trump suits.
 *
 */
public class TrickEvaluator {

    private final Suit leadSuit;
    private final Suit trumpSuit; // Can be null for Miserie

    /**
     * @throws IllegalArgumentException if lead suit is null
     */
    public TrickEvaluator(Suit leadSuit, Suit trumpSuit) {
        if (leadSuit == null) {throw new  IllegalArgumentException("leadSuit must not be null");}
        this.leadSuit = leadSuit;
        this.trumpSuit = trumpSuit;
    }

    /**
     * Determines if a challenger card beats the currently winning card.
     * @throws IllegalArgumentException if currentBest or challenger is null
     */
    public boolean doesBeat(Card challenger, Card currentBest) {
        if (currentBest == null || challenger == null) throw new  IllegalArgumentException("currentBest or challenger must not be null.");

        if (isTrump(challenger) || isTrump(currentBest)) {
            return evaluateTrumpContest(challenger, currentBest);
        }

        if (challenger.suit() == leadSuit) {
            return challenger.rank().compareTo(currentBest.rank()) > 0;
        }

        return false;
    }

    // --- Private Helper Methods for Readability ---

    private boolean isTrump(Card card) {
        return trumpSuit != null && card.suit() == trumpSuit;
    }

    private boolean evaluateTrumpContest(Card challenger, Card currentBest) {
        // Trump always beats a non-trump
        if (isTrump(challenger) && !isTrump(currentBest)) return true;
        if (!isTrump(challenger) && isTrump(currentBest)) return false;

        // If both are trumps, the higher rank wins
        return challenger.rank().compareTo(currentBest.rank()) > 0;
    }
}