package base;

import base.domain.WhistGame;
import base.domain.commands.GameCommand;
import base.domain.results.GameResult;
import base.domain.states.StateStep;
import cli.adapter.AdapterResponse;
import cli.adapter.AdapterResult;
import cli.events.IOEvent;
import cli.TerminalManager;
import cli.elements.Response;
import cli.adapter.Adapter;
import cli.MenuFlow;

/**
 * The main execution engine of the Whist application.
 * 
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

            GameCommand command = null;
            boolean stateRunning = true;

            while (!game.isOver()) {
                while (stateRunning) {
                    StateStep step;
                    if (command == null) {
                        step = game.executeState();
                    } else {
                        step = game.executeState(command);
                    }

                    stateRunning = !step.shouldTransition();

                    if (!step.hasResult()) {
                        command = null;
                        continue;
                    }

                    GameResult result = step.result();
                    // Adapter Conversion
                    AdapterResult adapterResult = adapter.handleResult(result);
                    // IO and resolution of Command
                    switch (adapterResult) {
                        // Bot turn: no IO needed, command is ready immediately
                        case AdapterResult.Immediate immediate -> command = immediate.command();
                        case AdapterResult.NeedsIO needsIO -> {
                            // Render and await all preamble events first
                            for (IOEvent pre : needsIO.preamble()) {
                                terminalManager.handle(pre);
                            }

                            IOEvent event = needsIO.event();

                            Response response = terminalManager.handle(event);
                            AdapterResponse adapterResponse = adapter.handleResponse(response, result);

                            while (adapterResponse.shouldReRenderLastResult()) {
                                for (IOEvent immediate : adapterResponse.immediateEvents()) {
                                    terminalManager.handle(immediate);
                                }
                                Response retryResponse = terminalManager.handle(event);
                                adapterResponse = adapter.handleResponse(retryResponse, result);
                            }

                            command = adapterResponse.command();
                        }
                        default -> throw new IllegalStateException("Unexpected value: " + adapterResult);
                    }
                }
                // State Transition
                game.nextState();
                stateRunning = true;
                command = null; // reset command for next state
            }

        }
    }
}