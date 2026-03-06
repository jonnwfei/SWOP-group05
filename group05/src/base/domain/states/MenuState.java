package base.domain.states;

import base.domain.WhistGame;
import base.domain.player.HighBotStrategy;
import base.domain.player.HumanStrategy;
import base.domain.player.LowBotStrategy;
import base.domain.player.Player;
import cli.elements.GameEvent;
import cli.elements.PromptElement;
import cli.elements.TextElement;

public class MenuState extends State {
    private int promptCount;
    private String keuze;
    private int totalBots;
    private int botCount; //bots added
    private int humanCount; // amount of players added

    public MenuState(WhistGame game){
        super(game);
        this.promptCount = 0;
        this.botCount = 0;
        this.humanCount = 0;
    }
    /*
     * In this function : we will initiate the game, by asking for the player names, and requesting the usecase
     * */
    @Override
    public GameEvent executeState(String input) {
        if (promptCount == 0) { // initial message
            String firstMsg = "======== WELCOME TO WHIST ===== \n " +
                    "Do you want to: \n"+
                    "(1) Play a game? \n"+
                    "(2) Count the scores for a game? ";
            promptCount += 1;
            return new PromptElement(firstMsg);
        }
        else if (promptCount == 1){ //getting al the players
            //first time we get here, in the input we have the choice
            keuze = input;
            promptCount += 1;
            String amountOfBots = "How many bots will be playing? ";
            return new PromptElement(amountOfBots);
        }
        else if (promptCount == 2) { //delegating the amount of bots
            totalBots = Integer.parseInt(input);
            promptCount += 1;
            return new PromptElement("Give the name of player one: ");
        }
        else {
            if (humanCount < (4 - totalBots)) {
                Player player = new Player(new HumanStrategy(), input);
                getGame().addPlayer(player);
                humanCount++;

                if (humanCount < (4 - totalBots)) {
                    return new PromptElement("Give the name of player " + (humanCount + 1) + ": ");
                } else  {
                    String botStrat= "(1) High Bot \n"+
                            "(2) Low Bot";
                    return new PromptElement("Which strategy should bot 1 use?: \n" + botStrat);
                }
            }
            // THEN: configure bots
            else {
                Player bot = null;
                if (input.equals("1")){
                    bot = new Player(new HighBotStrategy(), "Bot" + (botCount + 1));
                }
                else {
                    bot = new Player(new LowBotStrategy(), "Bot" + (botCount + 1));
                }
                getGame().addPlayer(bot);
                botCount++;
                if (botCount < totalBots) {
                    return new PromptElement("Which strategy should bot " + (botCount + 1) + " use?");
                } else {
                    //all players and bots added
                    return new TextElement(getGame().printNames());
                }
            }
        }
    }

    @Override
    public State nextState(){
       int keuzeInt = Integer.parseInt(keuze);
       if (keuzeInt == 1){
           return new BidState(getGame());
       }
       else{
           return new CountState(getGame());
       }
    }
}
