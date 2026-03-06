package base.domain.states;

import base.domain.WhistGame;

import cli.elements.GameEvent;
import cli.elements.QuestionEvent;

/**
* @author Stan Kestens
 *@since 06/03
 * In this State,
*/
public class CountState extends State {
    private Boolean bidASked;
    private Boolean trumpAsked;
    public CountState(WhistGame game){
        super(game);
        this.bidASked = false;
        this.trumpAsked = false;
    }
    @Override
    public GameEvent executeState(String s) {
        if (!bidASked){
            String askBid = "===== WELCOME TO THE COUNT==== \n" + "WHICH ROUND WOULD YOU LIKE TO PLAY \n" +
                    "Proposal: \n"+
                    "(1) Alonen      (2) With Partner\n "+
                    "Abondance (amount of cards)\n"+
                    "(3) 9   (4) 10   (5) 11   (6) 12\n"+
                    "Miserie\n"+
                    "(7) Normal       (8) Open          \n"+
                    "Solo\n"+
                    " (9) Normal       (10) Slim          \n";
            return new QuestionEvent(askBid);
        }
        else{
            return null;
        }
    }
    @Override
    public State nextState(){
        return null;
    }
}