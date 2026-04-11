package base;

import base.domain.WhistGame;
import base.domain.commands.ContinueCommand;
import base.domain.commands.GameCommand;
import base.domain.commands.StartGameCommand;
import base.domain.results.GameResult;
import cli.events.IOEvent;
import cli.TerminalManager;
import cli.elements.Response;
import cli.Adapter;
import cli.MenuFlow;

/**
 * The main execution engine of the Whist application.
 * @author Stan Kestens
 * @since 01/03/2026
 */
public class GameController {
    private final WhistGame game;
    private final TerminalManager terminalManager;
    private final Adapter adapter;
    private final MenuFlow menuFlow;

    public GameController() {
        this.game = new WhistGame();
        this.terminalManager = new TerminalManager();
        this.adapter = new Adapter(this.game);
        this.menuFlow = new MenuFlow(terminalManager, game);
    }

    public void run() {
        menuFlow.run(); // only once, before everything

        GameCommand command = new ContinueCommand();
        boolean stateRunning = true;

        while (!game.isOver()) {
            while (stateRunning) {
                GameResult result = game.executeState(command);
                IOEvent event = adapter.handleResult(result);
                stateRunning = event.getContinue();
                Response response = terminalManager.handle(event);
                command = adapter.handleResponse(response, result);
            }

            game.nextState();
            stateRunning = true;
            command = new ContinueCommand();
        }
    }
}