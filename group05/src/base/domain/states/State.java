package base.domain.states;

import base.domain.WhistGame;
import base.domain.actions.GameAction;
import base.domain.events.GameEvent;

/**
 * Common interface for all states.
 */
public abstract class State {
    private WhistGame game;
    public State(WhistGame game){
        this.game = game;
    }
    public WhistGame getGame(){
        return this.game;
    }
    /**
     * Context passes itself through the state constructor. This may help a
     * state to fetch some useful context data if needed.
     */
    public abstract State nextState();
    public abstract GameEvent executeState(GameAction action);
}