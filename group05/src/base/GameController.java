package base;

import base.domain.WhistGame;
import base.domain.commands.GameCommand;
import base.domain.commands.StartGameCommand;
import base.domain.results.GameResult;
import cli.events.IOEvent;
import cli.TerminalManager;
import cli.elements.Response;
import cli.Adapter;

/**
 * The main execution engine of the Whist application.
 * @author Stan Kestens
 * @since 01/03/2026
 */
public class GameController {
    private final WhistGame game;
    private final TerminalManager terminalManager;
    private Boolean isRunning;
    private Adapter adapter;
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
            GameCommand command = new StartGameCommand();

            while (state_running) {
                GameResult result = game.executeState(command);
                IOEvent event = adapter.handleResult(result);
                state_running = event.getContinue();
                Response response = terminalManager.handle(event);
                command = adapter.handleResponse(response, result);
            }

            game.nextState();
        }
    }
}