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
            switch (choice) {
                case 1 -> {
                    if (controller.getAllPlayers().size() > 4) controller.rotateActivePlayers();
                    if (mode == SaveMode.COUNT) {
                        controller.startCount();
                    } else {
                        controller.startGame();
                        controller.advanceDealer();
                    }
                    return true;
                }
                case 2 -> { return false; }
                case 3 -> editFlow.saveGame();
                case 4 -> editFlow.removeRound();
                case 5 -> editFlow.addPlayer();
                case 6 -> {
                    if (controller.canRemovePlayer()) editFlow.removePlayer();
                    else showScoreTable();
                }
                case 7 -> showScoreTable(); // only reachable when canRemove is true
                default -> throw new IllegalStateException("Unexpected value: " + choice);
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
        boolean       canRemove = controller.canRemovePlayer();
        int max = canRemove ? 7 : 6;
        return terminalInputHelper.askInt(new ScoreBoardIOEvent(names, scores, canRemove), 1, max);
    }
}