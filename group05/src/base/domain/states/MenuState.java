package base.domain.states;

import base.domain.WhistGame;
import base.domain.actions.GameAction;
import base.domain.actions.NumberAction;
import base.domain.actions.TextAction;
import base.domain.events.ErrorEvent;
import base.domain.events.menuEvents.*;
import base.domain.events.menuEvents.BotStrategyEvent;
import base.domain.player.HighBotStrategy;
import base.domain.player.HumanStrategy;
import base.domain.player.LowBotStrategy;
import base.domain.player.Player;
import base.domain.events.GameEvent;
import base.domain.deck.Deck;

import java.util.List;
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
     * Executes the menu state
     *
     * @param action@return the next QuestionEvent or TextEvent
     * @throws IllegalStateException getting in an unknown state
     */
    @Override
    public GameEvent executeState(GameAction action) {
        try {
            switch (state) {
                case WELCOME:
                    return showWelcome();

                case CHOOSE_MODE:
                    return handleMainChoice(action);

                case CHOOSE_BOTS:
                    return handleBotAmount(action);

                case ENTER_HUMANS:
                    return handleHumanInput(action);

                case ENTER_BOTS:
                    return handleBotInput(action);

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

    private GameEvent handleMainChoice(GameAction action) {
        if (!(action instanceof NumberAction(int value))) {
            return new ErrorEvent(1, 2); // Min 0 tricks, Max 13 tricks
        }
        keuze = value;
        if (keuze < 1 || keuze > 2) {
            return new ErrorEvent(0, 1);
        }


        if (keuze == 1) {
            state = SetupState.CHOOSE_BOTS;
            return new AmountOfBotsEvent();
        } else {
            totalBots = 0;
            state = SetupState.ENTER_HUMANS;
            return new PlayerNameEvent(1);
        }
    }

    private GameEvent handleBotAmount(GameAction action) {
        if (!(action instanceof NumberAction(int bots))) {
            return new ErrorEvent(0, 4);
        }
        if (bots < 0 || bots > 4) {
            return new ErrorEvent(0, 4);
        }


        totalBots = bots;
        state = SetupState.ENTER_HUMANS;
        return new PlayerNameEvent(1);
    }

    private GameEvent handleHumanInput(GameAction action) {
        if (!(action instanceof TextAction(String name))) {
            return new PrintNamesEvent(getPlayerNames()); //should return some error event
        }

        getGame().addPlayer(new Player(new HumanStrategy(), name));
        humanCount++;

        if (humanCount < (4 - totalBots)) {
            return new PlayerNameEvent(humanCount+1);
        }

        if (totalBots > 0) {
            state = SetupState.ENTER_BOTS;
            return new BotStrategyEvent(1);
        }


        return new PrintNamesEvent(getPlayerNames());
    }

    private GameEvent handleBotInput(GameAction action) {

        if (!(action instanceof NumberAction(int strategy))) {
            return new ErrorEvent(1, 2); // Min 0 tricks, Max 13 tricks
        }
        if (strategy < 1 || strategy > 2) {
            return new ErrorEvent(1, 2);
        }

        Player bot = strategy == 1
                ? new Player(new HighBotStrategy(), "Bot" + (botCount + 1))
                : new Player(new LowBotStrategy(), "Bot" + (botCount + 1));

        getGame().addPlayer(bot);
        botCount++;

        if (botCount < totalBots) {
            return new BotStrategyEvent(1);
        }

        return new PrintNamesEvent(getPlayerNames());
    }

    private List<String> getPlayerNames() {
        return getGame().getPlayers().stream().map(Player::getName).toList();
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
