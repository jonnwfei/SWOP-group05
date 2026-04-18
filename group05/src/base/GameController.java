package base;

import base.domain.WhistGame;
import base.domain.commands.GameCommand;
import base.domain.results.EndOfRoundResult;
import base.domain.results.GameResult;
import base.domain.states.StateStep;
import base.storage.GamePersistenceService;
import cli.adapter.Adapter;
import cli.adapter.AdapterResponse;
import cli.adapter.AdapterResult;
import cli.GameEditFlow;
import cli.MenuFlow;
import cli.ScoreBoardFlow;
import cli.TerminalManager;
import cli.elements.Response;
import cli.events.IOEvent;

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
    private final GameEditFlow editFlow;
    private final ScoreBoardFlow scoreBoardFlow;

    public GameController() {
        this.game = new WhistGame();
        this.terminalManager = new TerminalManager();
        this.adapter = new Adapter(this.game);

        GamePersistenceService persistenceService = new GamePersistenceService();
        this.menuFlow = new MenuFlow(terminalManager, persistenceService, game);
        this.editFlow = new GameEditFlow(terminalManager, game, persistenceService);
        this.scoreBoardFlow = new ScoreBoardFlow(terminalManager, game, editFlow);
    }

    public void run() {
        while (true) {
            menuFlow.run();

            // Inner loop: state machine + between-rounds scoreboard flow
            while (!game.isOver()) {
                boolean roundEnded = runStateMachine();

                if (roundEnded) {
                    boolean continuePlaying = scoreBoardFlow.run();
                    if (!continuePlaying) break;
                    game.startGame();
                }
            }
        }
    }

    /**
     * Drives the state machine until the current state is exhausted.
     *
     * @return true if the state machine exited because the round ended
     *         (and the scoreboard flow should take over), false if the
     *         whole game is done (count-mode, quit, ...).
     */
    private boolean runStateMachine() {
        GameCommand command = null;
        boolean stateRunning = true;
        boolean sawEndOfRound = false;

        while (stateRunning) {
            StateStep step = (command == null) ? game.executeState() : game.executeState(command);
            stateRunning = !step.shouldTransition();

            if (step.hasResult() && step.result() instanceof EndOfRoundResult) {
                sawEndOfRound = true;
            }

            if (!step.hasResult()) {
                command = null;
                continue;
            }

            command = resolveCommand(step.result());
        }

        game.nextState();
        return sawEndOfRound && game.isOver();
    }

    /**
     * Renders the result, collects input, and returns the resulting domain
     * command (or null if no command is needed).
     */
    private GameCommand resolveCommand(GameResult result) {
        AdapterResult adapterResult = adapter.handleResult(result);

        return switch (adapterResult) {
            case AdapterResult.Immediate immediate -> immediate.command();
            case AdapterResult.NeedsIO needsIO -> handleIO(needsIO, result);
        };
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
            Response retry = terminalManager.handle(event);
            adapterResponse = adapter.handleResponse(retry, result);
        }
        return adapterResponse.command();
    }
}