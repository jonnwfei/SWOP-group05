package base.domain.states;

import base.domain.WhistGame;

import cli.elements.GameEvent;

public class CountState extends State {
    public CountState(WhistGame game){
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