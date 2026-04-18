package base;

import base.domain.WhistGame;
import base.domain.commands.GameCommand;
import base.domain.results.GameResult;
import base.domain.states.StateStep;
import base.storage.GamePersistenceService;
import base.storage.snapshots.SaveMode;
import cli.GameEditFlow;
import cli.MenuFlow;
import cli.ScoreBoardFlow;
import cli.TerminalManager;
import cli.adapter.Adapter;
import cli.adapter.AdapterResponse;
import cli.adapter.AdapterResult;
import cli.elements.Response;
import cli.events.IOEvent;

public class GameController {

    private final WhistGame game;
    private final TerminalManager terminalManager;
    private final Adapter adapter;
    private final MenuFlow menuFlow;
    private final GamePersistenceService persistenceService;

    public GameController() {
        this.game = new WhistGame();
        this.terminalManager = new TerminalManager();
        this.adapter = new Adapter(this.game);
        this.persistenceService = new GamePersistenceService();
        this.menuFlow = new MenuFlow(terminalManager, persistenceService, game);
    }

    public void run() {
        while (true) {
            SaveMode mode = menuFlow.run(); // returns COUNT or GAME

            GameEditFlow editFlow = new GameEditFlow(terminalManager, game, persistenceService, mode);
            ScoreBoardFlow scoreBoardFlow = new ScoreBoardFlow(terminalManager, game, editFlow);

            runStateMachine();
            scoreBoardFlow.run();
        }
    }

    private void runStateMachine() {
        GameCommand command = null;
        boolean stateRunning = true;

        while (!game.isOver()) {
            while (stateRunning) {
                StateStep step = (command == null)
                        ? game.executeState()
                        : game.executeState(command);

                stateRunning = !step.shouldTransition();

                if (!step.hasResult()) {
                    command = null;
                    continue;
                }

                GameResult result = step.result();
                AdapterResult adapterResult = adapter.handleResult(result);

                command = switch (adapterResult) {
                    case AdapterResult.Immediate immediate -> immediate.command();
                    case AdapterResult.NeedsIO needsIO    -> handleIO(needsIO, result);
                };
            }

            game.nextState();
            stateRunning = true;
            command = null;
        }
    }

    private GameCommand handleIO(AdapterResult.NeedsIO needsIO, GameResult result) {
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
            response = terminalManager.handle(event);
            adapterResponse = adapter.handleResponse(response, result);
        }

        return adapterResponse.command();
    }
}