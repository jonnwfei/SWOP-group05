package base;
import base.domain.WhistGame;
import cli.TerminalManager;
import cli.elements.GameEvent;
import cli.elements.Response;

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
                GameEvent response = game.executeState(answer);
                Response response = terminalManager.handle(response); // response : boolean keeprunnig, answer str
                game_running = response.get_contiue();
                answer = response.get_anszer();
            }
            game.nextState(answer);
        }
    }

}