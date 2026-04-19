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
    /**
     * Creates a new ScoreBoardFlow.
     *
     * @param terminalManager handles user interaction
     * @param game the current game instance
     * @param editFlow shared edit functionality
     */
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
     * Runs the scoreboard loop until the user exits or continues.
     *
     * @param mode current game mode
     * @return true to continue playing, false to return to menu
     */
    public boolean run(SaveMode mode) {
        while (true) {
            int choice = showMenu();
            switch (choice) {
                case 1 -> {
                    if (mode == SaveMode.COUNT){
                        game.startCount();
                    }
                    else{
                        game.startGame();
                    }
                    if (game.getAllPlayers().size() > 4) {
                        game.rotateActivePlayers();
                    }
                    return true; }   // another round
                case 2 -> { return false; }  // main menu
                case 3 -> { editFlow.saveGame(); } // save then re-show
                case 4 -> { editFlow.removeRound();}
                case 5 -> { editFlow.addPlayer();  }
                case 6 -> { editFlow.removePlayer(); }
            }
        }
    }
    /**
     * Displays the scoreboard and menu options.
     *
     * @return selected menu option
     */
    private int showMenu() {
        List<String> names = game.getAllPlayers().stream().map(Player::getName).toList();
        List<Integer> scores = game.getAllPlayers().stream().map(Player::getScore).toList();
        boolean canRemove = game.canRemovePlayer();
        int max = canRemove ? 6 : 5;
        return askInt(new ScoreBoardIOEvent(names, scores, canRemove), 1, max);
    }
    /**
     * Reads an integer within a range.
     *
     * @param event IO event to display
     * @param min minimum value
     * @param max maximum value
     * @return validated integer
     */
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