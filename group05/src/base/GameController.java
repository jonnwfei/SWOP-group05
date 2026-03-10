package base;
import base.domain.WhistGame;
import base.domain.actions.ContinueAction;
import base.domain.actions.GameAction;
import cli.TerminalManager;
import base.domain.events.GameEvent;
import cli.elements.Response;
/**
 * @author stankestens
* @since 01/03
 *
* */
public class GameController {
    private final WhistGame game;
    private final TerminalManager terminalManager;
    private Boolean isRunning;
    public GameController(){
        this.game = new WhistGame();
        this.terminalManager = new TerminalManager();
        this.isRunning = true;
    }


    public void run(){
        while(isRunning) {
            Boolean state_running = true;
            GameAction answer = new ContinueAction();
            while (state_running) {
                GameEvent event = game.executeState(answer);
                Response response = terminalManager.handle(event); // response : boolean keeprunnig, answer str
                state_running = response.getContinue();
                answer = response.getAction();
            }
            game.nextState();
        }
    }

}