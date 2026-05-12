package cli.history.historyActions;

import base.domain.bid.BidType;
import cli.history.ReversibleAction;
/*
* Beetje voor future proofing deze, want deze logica is zlef nog niet geimplemnteerd
*
* */
/*
public class ChangeScoringAction implements ReversibleAction {
    private final ScoringRules rules;
    private final BidType bidType;
    private final int oldValue;
    private final int newValue;

    @Override public void execute() {
        rules.setValue(bidType, newValue);
        controller.recalibrateScores();
    }

    @Override public void undo() {
        rules.setValue(bidType, oldValue);
        controller.recalibrateScores();
    }
}
*/
