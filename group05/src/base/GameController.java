package base;
import base.domain.WhistGame;
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
        while(true) {
            Boolean game_running = true;
            String answer = "";
            while (game_running) {
                GameEvent event = game.executeState(answer);
                Response response = terminalManager.handle(event); // response : boolean keeprunnig, answer str
                game_running = response.getContinue();
                answer = response.getContent();
            }
            game.nextState();
        }
    }

}