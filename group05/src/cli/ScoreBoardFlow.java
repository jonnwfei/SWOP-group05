package cli;

import base.domain.WhistGame;
import base.domain.player.Player;
import base.storage.snapshots.SaveMode;
import cli.events.IOEvent;

import static cli.events.CountEvents.ScoreBoardIOEvent;

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
    private final WhistGame game;
    private final GameEditFlow editFlow;

    public ScoreBoardFlow(TerminalManager terminalManager, WhistGame game, GameEditFlow editFlow) {
        if (terminalManager == null)
            throw new IllegalArgumentException("terminalManager cannot be null");
        if (game == null)
            throw new IllegalArgumentException("game cannot be null");
        if (editFlow == null)
            throw new IllegalArgumentException("editFlow cannot be null");
        this.terminalManager = terminalManager;
        this.game = game;
        this.editFlow = editFlow;
    }

    /**
     * Runs the scoreboard loop until the user chooses to continue or quit.
     *
     * @return true to play another round, false to exit to the main menu
     */
    public boolean run() {
        while (true) {
            int choice = showMenu();
            switch (choice) {
                case 1 -> {
                    game.advanceDealer();
                    return true;
                }
                case 2 -> { return false; }
                case 3 -> editFlow.saveGame();
                case 4 -> editFlow.removeRound();
                case 5 -> editFlow.addPlayer();
                case 6 -> editFlow.removePlayer();
            }
        }
    }

    private int showMenu() {
        List<String> names = game.getPlayers().stream().map(Player::getName).toList();
        List<Integer> scores = game.getPlayers().stream().map(Player::getScore).toList();
        boolean canRemove = game.canRemovePlayer();
        int max = canRemove ? 6 : 5;
        return askInt(new ScoreBoardIOEvent(names, scores, canRemove), 1, max);
    }

    private int askInt(IOEvent event, int min, int max) {
        while (true) {
            try {
                String raw = terminalManager.handle(event).rawInput();
                int value = Integer.parseInt(raw.trim());
                if (value >= min && value <= max) return value;
                System.out.println("Please enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }
}