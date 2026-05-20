package base.commands.actions;

import base.commands.ReversibleAction;
import base.domain.WhistGame;
import base.domain.round.Round;

/**
 * Reversible action that removes a {@link Round} from a {@link WhistGame}.
 * The round's original position is remembered so the undo can restore it
 * to the same slot, and scores are recalibrated after every change.
 */
public class RemoveRoundAction implements ReversibleAction {
    private final WhistGame game;
    private final Round round;
    private final int originalIndex;

    /**
     * @param originalIndex the zero-based index of {@code round} in the game's round list;
     *                      must be {@code >= 0}
     * @param game          the game to remove the round from; must not be {@code null}
     * @param round         the round to remove; must not be {@code null}
     * @throws IllegalArgumentException if any argument is {@code null}, or if
     *                                  {@code originalIndex} is negative
     */
    public RemoveRoundAction(WhistGame game, Round round, int originalIndex) {
        if (game  == null) throw new IllegalArgumentException("game cannot be null");
        if (round == null) throw new IllegalArgumentException("round cannot be null");
        if (originalIndex < 0) throw new IllegalArgumentException("originalIndex cannot be negative");
        this.game          = game;
        this.round         = round;
        this.originalIndex = originalIndex;
    }

    /** Removes the round from the game and recalibrates all scores. */
    @Override
    public void execute() {
        game.removeRound(round);
        game.recalibrateScores();
    }

    /** Re-inserts the round at its original position and recalibrates all scores. */
    @Override
    public void undo() {
        game.addRoundAtIndex(round, originalIndex);
        game.recalibrateScores();
    }
}