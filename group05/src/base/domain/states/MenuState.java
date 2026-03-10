package base.domain.states;

import base.domain.WhistGame;
import base.domain.events.menuEvents.PrintNamesEvent;
import base.domain.events.menuEvents.AmountOfBotsEvent;
import base.domain.events.menuEvents.BotStategyEvent;
import base.domain.events.menuEvents.PlayerNameEvent;
import base.domain.events.menuEvents.WelcomeMenuEvent;
import base.domain.player.HighBotStrategy;
import base.domain.player.HumanStrategy;
import base.domain.player.LowBotStrategy;
import base.domain.player.Player;
import base.domain.events.GameEvent;
import base.domain.events.QuestionEvent;
import base.domain.deck.Deck;

import java.util.Random;

public class MenuState extends State {
    private int keuze;
    private int totalBots;
    private int botCount; //bots added
    private int humanCount; // amount of players added

    public MenuState(WhistGame game){
        super(game);
        this.botCount = 0;
        this.humanCount = 0;
    }
    /*
     * In this function : we will initiate the game, by asking for the player names, and requesting the usecase
     * */
    private enum SetupState {
        WELCOME,
        CHOOSE_MODE,
        CHOOSE_BOTS,
        ENTER_HUMANS,
        ENTER_BOTS
    }

    private SetupState state = SetupState.WELCOME;

    /**
     *Executes the menu state
     *
     * @param  input the users response to the previous QuestionEvent
     * @return the next QuestionEvent or TextEvent
     * @throws IllegalStateException getting in an unknown state
     */
    @Override
    public GameEvent executeState(String input) {
        try {
            switch (state) {
                case WELCOME:
                    return showWelcome();

                case CHOOSE_MODE:
                    return handleMainChoice(input);

                case CHOOSE_BOTS:
                    return handleBotAmount(input);

                case ENTER_HUMANS:
                    return handleHumanInput(input);

                case ENTER_BOTS:
                    return handleBotInput(input);

                default:
                    throw new IllegalStateException("Unexpected state: " + state);
            }
        } catch (IllegalArgumentException e) {
            // Now catching the specific error thrown by our parsers
            throw new IllegalStateException("Flow stuck in MenuState at phase " + state.name() + ": " + e.getMessage());
        }
    }

    private GameEvent showWelcome() {
        state = SetupState.CHOOSE_MODE;
        getGame().resetPlayers();
        getGame().resetRounds();
        return new WelcomeMenuEvent();
    }

    private GameEvent handleMainChoice(String input) {
        keuze = Integer.parseInt(input);


        if (keuze == 1) {
            state = SetupState.CHOOSE_BOTS;
            return new AmountOfBotsEvent();
        } else {
            totalBots = 0;
            state = SetupState.ENTER_HUMANS;
            return new PlayerNameEvent(1);
        }
    }

    private GameEvent handleBotAmount(String input) {
        int bots = Integer.parseInt(input);


        totalBots = bots;
        state = SetupState.ENTER_HUMANS;
        return new PlayerNameEvent(1);
    }

    private GameEvent handleHumanInput(String input) {

        getGame().addPlayer(new Player(new HumanStrategy(), input));
        humanCount++;

        if (humanCount < (4 - totalBots)) {
            return new PlayerNameEvent(humanCount+1);
        }

        if (totalBots > 0) {
            state = SetupState.ENTER_BOTS;
            return new BotStategyEvent(1);
        }

        return new PrintNamesEvent(getGame().getPlayers());
    }

    private GameEvent handleBotInput(String input) {
        if (!input.equals("1") && !input.equals("2")) {
            return new QuestionEvent("Invalid strategy. Choose (1) High Bot or (2) Low Bot:");
        }

        Player bot = input.equals("1")
                ? new Player(new HighBotStrategy(), "Bot" + (botCount + 1))
                : new Player(new LowBotStrategy(), "Bot" + (botCount + 1));

        getGame().addPlayer(bot);
        botCount++;

        if (botCount < totalBots) {
            return new BotStategyEvent(1);
        }

        return new PrintNamesEvent(getGame().getPlayers());
    }




    @Override
    public State nextState(){
       if (keuze == 1){
           getGame().setDeck(new Deck());
           getGame().setRandomDealer();
           return new BidState(getGame());
       }
       else{
           return new CountState(getGame());
       }
    }
}
