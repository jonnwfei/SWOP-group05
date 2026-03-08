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
        try {
            if (promptCount == 0) {
                String firstMsg = "======== WELCOME TO WHIST ===== \n" +
                        "Do you want to: \n" +
                        "(1) Play a game? \n" +
                        "(2) Count the scores for a game? \n";
                promptCount = 1; // Direct naar de eerste input-stap
                return new QuestionEvent(firstMsg);
            }

            if (promptCount == 1) {
                keuze = Integer.parseInt(input);
                if (keuze != 1 && keuze != 2) {
                    return new QuestionEvent("Invalid choice. Please enter (1) or (2):");
                }
                promptCount = 2;
                if (keuze == 1) {
                    return new QuestionEvent("How many bots will be playing? (0-4):");
                } else {
                    totalBots = 0;
                    promptCount = 3; // Skip bot-hoeveelheid vraag voor counting
                    return new QuestionEvent("Give the name of player 1: ");
                }
            }

            if (promptCount == 2) { // Delegating amount of bots
                int bots = Integer.parseInt(input);
                if (bots < 0 || bots > 4) {
                    return new QuestionEvent("Invalid amount. Enter a number between 0 and 4:");
                }
                totalBots = bots;
                promptCount = 3;
                return new QuestionEvent("Give the name of player 1: ");
            }


            if (promptCount >= 3) {
                if (keuze == 1) {
                    // Adding human players
                    if (humanCount < (4 - totalBots)) {
                        if (input.trim().isEmpty()) return new QuestionEvent("Name cannot be empty. Give player " + (humanCount + 1) + " a name:");
                        getGame().addPlayer(new Player(new HumanStrategy(), input));
                        humanCount++;

                        if (humanCount < (4 - totalBots)) {
                            return new QuestionEvent("Give the name of player " + (humanCount + 1) + ": ");
                        } else if (totalBots > 0) {
                            return new QuestionEvent("Which strategy should bot 1 use?: \n(1) High Bot \n(2) Low Bot\n");
                        } else {
                            return new TextEvent(getGame().printNames());
                        }
                    }
                    // Adding bots
                    else {
                        if (!input.equals("1") && !input.equals("2")) {
                            return new QuestionEvent("Invalid strategy. Choose (1) High Bot or (2) Low Bot:");
                        }
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
                // Flow voor alleen counting
                else {
                    if (input.trim().isEmpty()) return new QuestionEvent("Name cannot be empty. Give player " + (humanCount + 1) + " a name:");
                    getGame().addPlayer(new Player(new HumanStrategy(), input));
                    humanCount++;

                    if (humanCount < 4) {
                        return new QuestionEvent("Give the name of player " + (humanCount + 1) + ": ");
                    } else {
                        return new TextEvent(getGame().printNames());
                    }
                }
            }

        } catch (NumberFormatException e) {
            return new QuestionEvent("That's not a valid number. Please try again:");
        }

        return new TextEvent("System Error: Flow stuck."); // Fallback
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
