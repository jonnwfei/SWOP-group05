package base.domain.states;

import base.domain.WhistGame;

import cli.elements.GameEvent;
import cli.elements.PromptElement;
import cli.elements.TextElement;

public class BidState extends State {
    public BidState(WhistGame game){
        super(game);
    }
    @Override
    public GameEvent executeState(String s) {
        return null;
    }
    @Override
    public State nextState(){
        return null;
    }
}

