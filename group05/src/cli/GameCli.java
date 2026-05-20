package cli;
import base.GameController;
import base.domain.WhistGame;
import base.domain.commands.GameCommand;
import base.storage.GamePersistenceService;
import base.domain.snapshots.SaveMode;
import cli.adapter.Adapter;
import cli.flows.GameEditFlow;
import cli.flows.MenuFlow;
import cli.flows.ScoreBoardFlow;
import base.domain.results.GameResult;
import cli.adapter.AdapterResponse;
import cli.adapter.AdapterResult;
import cli.elements.Response;
import cli.events.IOEvent;


/**
 * Application orchestrator — owns the session loop, menus, and IO.
 * Has no knowledge of StateStep, GameResult, or any domain mechanics.
 */
public class GameCli {
    private final GameController controller;
    private final Adapter adapter;
    private final TerminalManager terminalManager;
    private final MenuFlow menuFlow;
    private final GamePersistenceService persistenceService;

    public GameCli() {
        this.controller = new GameController(new WhistGame());
        this.adapter = new Adapter(controller);
        this.terminalManager = new TerminalManager();
        this.persistenceService = new GamePersistenceService();
        this.menuFlow = new MenuFlow(terminalManager, persistenceService, controller);
    }

    public void run() {
        while (true) {
            controller.reset();
            SaveMode mode = menuFlow.run();
            controller.clearHistory();
            GameEditFlow editFlow = new GameEditFlow(terminalManager, controller, persistenceService, mode);
            ScoreBoardFlow scoreBoardFlow = new ScoreBoardFlow(terminalManager, controller, editFlow);

            boolean continueSession = true;
            while (continueSession) {
                runSession();
                continueSession = scoreBoardFlow.run(mode);
            }
        }
    }

    private void runSession() {
        GameResult result = controller.advance(null);
        while (!controller.isGameOver()) {
            GameCommand command = handleIO(result);
            result = controller.advance(command);
        }
    }

    private GameCommand handleIO(GameResult result) {
        AdapterResult adapterResult = adapter.handleResult(result);
        return switch (adapterResult) {
            case AdapterResult.Immediate immediate -> immediate.command();
            case AdapterResult.NeedsIO needsIO    -> promptUser(needsIO, result);
        };
    }

    private GameCommand promptUser(AdapterResult.NeedsIO needsIO, GameResult result) {
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













