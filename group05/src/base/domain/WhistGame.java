package base.domain;
import java.util.List;
import base.domain.player.*;
import base.domain.round.Round;
import cli.elements.GameEvent;
import cli.elements.PromptElement;
import cli.elements.TextElement;

public class WhistGame {

    private State state;
    private boolean running;
    private List<Player> players;
    private List<Round> rounds;
    private Player currentPlayer;
    private Player dealerPlayer;

    private abstract class State {
        abstract GameEvent executeState(String response);

    }
    private class MenuState extends State {
        private int promptcount = 0;
        @Override
        GameEvent executeState(String s) {
            if (promptcount == 0) {
                return new PromptElement("This is a question? : ");
            }
            else{
                return new TextElement("This is a test!");
            }
        }
    }
    private class BidState extends State {
        private int promptcount = 0;
        @Override
        GameEvent executeState(String s) {
            if (promptcount == 0) {
                return new PromptElement("This is a question? : ");
            }
            else{
                return new TextElement("This is a test!");
            }
        }


    }
    private class PlayState extends State {
        private int promptcount = 0;
        @Override
        GameEvent executeState(String s) {
            if (promptcount == 0) {
                return new PromptElement("This is a question? : ");
            }
            else{
                return new TextElement("This is a test!");
            }
        }

    }
    private class CountState extends State {

        private int promptcount = 0;
        @Override
        GameEvent executeState(String s) {
            if (promptcount == 0) {
                return new PromptElement("This is a question? : ");
            }
            else{
                return new TextElement("This is a test!");
            }
        }
    }
    public void nextState(){

    }

}
