package base;

import base.domain.WhistGame;
import base.domain.actions.ContinueAction;
import base.domain.actions.GameAction;
import cli.TerminalManager;
import base.domain.events.GameEvent;
import cli.elements.Response;

/**
 * The main execution engine of the Whist application.
 * @author Stan Kestens
 * @since 01/03/2026
 */
public class GameController {
    private final WhistGame game;
    private final TerminalManager terminalManager;
    private Boolean isRunning;

    /**
     * Initializes the controller with a new game instance and terminal handler.
     */
    public GameController(){
        this.game = new WhistGame();
        this.terminalManager = new TerminalManager();
        this.isRunning = true;
    }

    /**
     * Starts the main execution loop.
     */
    public void run(){
        while(isRunning) {
            Boolean state_running = true;
            GameAction answer = new ContinueAction();

            while (state_running) {
                GameEvent<?> event = game.executeState(answer);
                Response response = terminalManager.handle(event);
                state_running = response.getContinue();
                answer = response.getAction();
            }

            game.nextState();
        }
    }
}