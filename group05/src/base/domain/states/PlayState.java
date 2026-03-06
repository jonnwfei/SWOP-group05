package base.domain.states;

import base.domain.WhistGame;

import cli.elements.GameEvent;
import cli.elements.PromptElement;
import cli.elements.TextElement;
/*
* input :Game, bid,
*
* */
public class PlayState extends State {
    public PlayState(WhistGame game){
        super(game);
    }
    @Override
    public GameEvent executeState(String input) {
      return null;
    }
    @Override
    public State nextState(){
        return null;
    }
}