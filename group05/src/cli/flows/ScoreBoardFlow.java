package cli.flows;

import base.GameController;
import base.domain.round.Round;
import base.storage.snapshots.SaveMode;
import cli.TerminalManager;
import cli.events.MenuEvents.*;
import cli.util.TerminalInputHelper;
import cli.events.CountEvents.*;

import java.util.List;

/**
 * Between-rounds scoreboard menu. Delegates cross-cutting edit actions to
 * {@link GameEditFlow} and only owns its own top-level menu routing.
 *
 * @author John Cai
 * @since 18/04/2026
 */
public class ScoreBoardFlow {

    private final TerminalManager terminalManager;
    private final GameController controller;
    private final GameEditFlow editFlow;
    private final TerminalInputHelper terminalInputHelper;

    public ScoreBoardFlow(TerminalManager terminalManager, GameController controller, GameEditFlow editFlow) {
        if (terminalManager == null) throw new IllegalArgumentException("terminalManager cannot be null");
        if (controller == null)      throw new IllegalArgumentException("game cannot be null");
        if (editFlow == null)        throw new IllegalArgumentException("editFlow cannot be null");
        this.terminalManager     = terminalManager;
        this.controller          = controller;
        this.editFlow            = editFlow;
        this.terminalInputHelper = new TerminalInputHelper(this.terminalManager);
    }

    public boolean run(SaveMode mode) {
        while (true) {
            int choice = showMenu();
            boolean canRemove = controller.canRemovePlayer();
            boolean canUndo   = controller.canUndo();
            boolean canRedo   = controller.canRedo();

            // options 1-5 are fixed
            if      (choice == 1) {
                if (controller.getAllPlayers().size() > 4) controller.rotateActivePlayers();
                if (mode == SaveMode.COUNT) {
                    controller.startCount();
                } else {
                    controller.startGame();
                    controller.advanceDealer();
                }
                return true;
            }
            else if (choice == 2) { return false; }
            else if (choice == 3) { editFlow.saveGame(); }
            else if (choice == 4) { editFlow.removeRound(); }
            else if (choice == 5) { editFlow.addPlayer(); }
            else {
                // options 6+ shift depending on what's available
                int next = 6;
                if (canRemove && choice == next++) { editFlow.removePlayer(); continue; }
                if (choice == next++)              { showScoreTable();        continue; }
                if (canUndo   && choice == next++) { controller.undo();       continue; }
                if (canRedo   && choice == next)   { controller.redo();       continue; }
                throw new IllegalStateException("Unexpected value: " + choice);
            }
        }
    }

    private void showScoreTable() {
        List<String> playerNames = controller.getPlayerNames();
        List<Round>  rounds      = controller.getRounds();
        terminalManager.handle(new ScoreTableIOEvent(playerNames, rounds));
    }

    private int showMenu() {
        List<String>  names     = controller.getPlayerNames();
        List<Integer> scores    = controller.getPlayerScores();
        boolean canRemove       = controller.canRemovePlayer();
        boolean canUndo         = controller.canUndo();
        boolean canRedo         = controller.canRedo();

        int max = 6                              // options 1-6 always present
                + (canRemove ? 1 : 0)           // remove player
                + (canUndo   ? 1 : 0)
                + (canRedo   ? 1 : 0);

        return terminalInputHelper.askInt(
                new ScoreBoardIOEvent(names, scores, canRemove, canUndo, canRedo), 1, max);
    }
}