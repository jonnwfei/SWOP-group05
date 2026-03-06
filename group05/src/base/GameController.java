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
        this.game = new  ();
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
            game.nextState(answer);
        }
    }

}