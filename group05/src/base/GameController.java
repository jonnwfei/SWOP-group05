package base;

import base.domain.WhistGame;
import base.domain.commands.ContinueCommand;
import base.domain.commands.GameCommand;
import base.domain.results.GameResult;
import cli.adapter.AdapterResponse;
import cli.adapter.AdapterResult;
import cli.events.IOEvent;
import cli.TerminalManager;
import cli.elements.Response;
import cli.adapter.Adapter;
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
        boolean playAgain = true;

        while (playAgain) {

            menuFlow.run();

            GameCommand command = new ContinueCommand();
            boolean stateRunning = true;

            while (!game.isOver()) {
                while (stateRunning) {
                    GameResult result = game.executeState(command);

                    AdapterResult adapterResult = adapter.handleResult(result);

                    switch (adapterResult) {
                        case AdapterResult.Immediate immediate -> {
                            // Bot turn: no IO needed, command is ready immediately
                            stateRunning = true;
                            command = immediate.command();
                        }

                        case AdapterResult.NeedsIO needsIO -> {
                            IOEvent event = needsIO.event();
                            stateRunning = event.getContinue();

                            Response response = terminalManager.handle(event);
                            AdapterResponse adapterResponse = adapter.handleResponse(response, result);

                            while (adapterResponse.command() == null) {
                                for (IOEvent immediate : adapterResponse.immediateEvents()) {
                                    terminalManager.handle(immediate);
                                }
                                Response retryResponse = terminalManager.handle(event);
                                adapterResponse = adapter.handleResponse(retryResponse, result);
                            }

                            command = adapterResponse.command();
                        }
                    }
                }

                game.nextState();
                stateRunning = true;
                command = new ContinueCommand();
            }


        }
    }
}