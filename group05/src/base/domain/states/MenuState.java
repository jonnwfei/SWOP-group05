package base.domain.states;

import base.domain.WhistGame;
import base.domain.player.HighBotStrategy;
import base.domain.player.HumanStrategy;
import base.domain.player.LowBotStrategy;
import base.domain.player.Player;
import cli.elements.GameEvent;
import cli.elements.QuestionEvent;
import cli.elements.TextEvent;
import base.domain.deck.Deck;
public class MenuState extends State {
    private int promptCount;
    private int keuze;
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
                    "(2) Count the scores for a game? \n";
            promptCount += 1;
            return new QuestionEvent(firstMsg);
        }
        else if (promptCount == 1){ //getting al the players
            //first time we get here, in the input we have the choice
            keuze = Integer.parseInt(input);
            promptCount += 1;
            if (keuze == 1){
                String amountOfBots = "How many bots will be playing? ";
                return new QuestionEvent(amountOfBots);
            }
            else{
                totalBots = 0;
                promptCount += 1;
                return new QuestionEvent("Give the name of player 1: ");
            }

        }
        else if (promptCount == 2) { //delegating the amount of bots
            totalBots = Integer.parseInt(input);
            promptCount += 1;
            return new QuestionEvent("Give the name of player 1: ");
        }
        else {
            // --- FLOW FOR PLAYING (Choice 1) ---
            if (keuze == 1) {
                if (humanCount < (4 - totalBots)) {
                    getGame().addPlayer(new Player(new HumanStrategy(), input));
                    humanCount++;

                    // If we still need more humans
                    if (humanCount < (4 - totalBots)) {
                        return new QuestionEvent("Give the name of player " + (humanCount + 1) + ": ");
                    }
                    // If we have all humans but need bots
                    else if (totalBots > 0) {
                        return new QuestionEvent("Which strategy should bot 1 use?: \n(1) High Bot \n(2) Low Bot\n");
                    }
                    // If we have 4 humans and 0 bots, we are done
                    else {
                        return new TextEvent(getGame().printNames());
                    }
                } else {
                    // Logic for adding bots (only happens if keuze == 1)
                    Player bot = input.equals("1") ?
                            new Player(new HighBotStrategy(), "Bot" + (botCount + 1)) :
                            new Player(new LowBotStrategy(), "Bot" + (botCount + 1));

                    getGame().addPlayer(bot);
                    botCount++;

                    if (botCount < totalBots) {
                        return new QuestionEvent("Which strategy should bot " + (botCount + 1) + " use?\n(1) High Bot \n(2) Low Bot\n");
                    } else {
                        return new TextEvent(getGame().printNames());
                    }
                }
            }
            // --- FLOW FOR COUNTING (Choice 2) ---
            else {
                getGame().addPlayer(new Player(new HumanStrategy(), input));
                humanCount++;

                if (humanCount < 4) {
                    return new QuestionEvent("Give the name of player " + (humanCount + 1) + ": ");
                } else {
                    return new TextEvent(getGame().printNames());
                }
            }
        }
    }

    @Override
    public State nextState(){
       if (keuze == 1){
           getGame().setDeck(new Deck());
           return new BidState(getGame());
       }
       else{
           return new CountState(getGame());
       }
    }
}
