package base.commands.actions;

import base.commands.ReversibleAction;
import base.domain.WhistGame;
import base.domain.round.Round;

public class RemoveRoundAction implements ReversibleAction {
    private final WhistGame game;
    private final Round round;
    private final int originalIndex;

    public RemoveRoundAction(WhistGame game, Round round, int originalIndex) {
        this.game          = game;
        this.round         = round;
        this.originalIndex = originalIndex;
    }

    @Override
    public void execute() {
        game.removeRound(round);
        game.recalibrateScores();
    }

    @Override
    public void undo() {
        game.addRoundAtIndex(round, originalIndex);
        game.recalibrateScores();
    }
}