package cli.elements;

import base.domain.actions.GameAction;

public class Response{
    private final Boolean keepRunning;
    private final GameAction action;


    public Response(Boolean keepRunning, GameAction action){
        this.keepRunning = keepRunning;
        this.action = action;
    }

    public Boolean getContinue(){
        return this.keepRunning;
    }
    public GameAction getAction(){
        return this.action;
    }
}