package base.domain.states;

import base.domain.WhistGame;
import base.domain.actions.GameAction;
import base.domain.actions.NumberAction;
import base.domain.actions.TextAction;
import base.domain.events.ErrorEvent;
import base.domain.events.menuEvents.*;
import base.storage.GamePersistenceService;
import base.domain.snapshots.SaveMode;
import base.domain.player.HighBotStrategy;
import base.domain.player.HumanStrategy;
import base.domain.player.LowBotStrategy;
import base.domain.player.Player;
import base.domain.events.GameEvent;
import base.domain.deck.Deck;
import java.util.List;

/**
 * Handles the initial setup of the game, including mode selection and player
 * registration.
 *
 * @author Stan Kestens
 * @since 28/02/2026
 */
public class MenuState extends State {
    private int keuze;
    private int totalBots;
    private int botCount;
    private int humanCount;
    private State resumeTargetState;
    private List<String> availableSaves;
    private final GamePersistenceService persistenceService;

    /**
     * Defines the internal phases of the menu setup flow.
     */
    private enum SetupState {
        WELCOME,
        CHOOSE_MODE,
        RESUME_SELECT,
        CHOOSE_BOTS,
        ENTER_HUMANS,
        ENTER_BOTS
    }

    private SetupState state = SetupState.WELCOME;

    /**
     * Initializes the menu state with zeroed counters.
     * 
     * @param game The main game instance.
     */
    public MenuState(WhistGame game) {
        super(game);
        this.botCount = 0;
        this.humanCount = 0;
        this.resumeTargetState = null;
        this.availableSaves = List.of();
        this.persistenceService = new GamePersistenceService();
    }

    /**
     * Routes the user action to the appropriate internal setup handler.
     * 
     * @param action The user input (number or text).
     * @return The next event to display in the terminal.
     * @throws IllegalStateException if the flow encounters an unhandled setup
     *                               phase.
     */
    @Override
    public GameEvent<?> executeState(GameAction action) {
        try {
            switch (state) {
                case WELCOME -> {
                    return showWelcome();
                }
                case CHOOSE_MODE -> {
                    return handleMainChoice(action);
                }
                case RESUME_SELECT -> {
                    return handleResumeSelect(action);
                }
                case CHOOSE_BOTS -> {
                    return handleBotAmount(action);
                }
                case ENTER_HUMANS -> {
                    return handleHumanInput(action);
                }
                case ENTER_BOTS -> {
                    return handleBotInput(action);
                }
                default -> throw new IllegalStateException("Unexpected state: " + state);
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Flow stuck in MenuState at phase " + state.name() + ": " + e.getMessage());
        }
    }

    /**
     * Resets game data and prepares the main menu display.
     * 
     * @return GameEvent
     */
    private GameEvent<?> showWelcome() {
        state = SetupState.CHOOSE_MODE;
        getGame().resetPlayers();
        getGame().resetRounds();
        return new WelcomeMenuEvent();
    }

    /**
     * Processes the choice between playing a game or counting scores.
     * 
     * @return GameEvent
     */
    private GameEvent<?> handleMainChoice(GameAction action) {
        int value = switch (action) {
            case NumberAction(int selected) -> selected;
            default -> -1;
        };
        if (value == -1)
            return new ErrorEvent(1, 3);

        keuze = value;
        if (keuze < 1 || keuze > 3) {
            return new ErrorEvent(1, 3);
        }

        if (keuze == 1) {
            state = SetupState.CHOOSE_BOTS;
            return new AmountOfBotsEvent();
        } else if (keuze == 2) {
            totalBots = 0;
            state = SetupState.ENTER_HUMANS;
            return new PlayerNameEvent(1);
        }

        availableSaves = persistenceService.listDescriptions();
        if (availableSaves.isEmpty()) {
            state = SetupState.CHOOSE_MODE;
            return new WelcomeMenuEvent();
        }
        state = SetupState.RESUME_SELECT;
        return new ResumeSaveEvent(availableSaves);
    }

    private GameEvent<?> handleResumeSelect(GameAction action) {
        Integer selection = switch (action) {
            case NumberAction(int selected) -> selected;
            default -> null;
        };
        if (selection == null || selection < 1 || selection > availableSaves.size()) {
            return new ErrorEvent(1, availableSaves.size());
        }

        String description = availableSaves.get(selection - 1);
        SaveMode loadedMode = persistenceService.loadIntoGame(getGame(), description);
        if (loadedMode == null) {
            state = SetupState.CHOOSE_MODE;
            return new WelcomeMenuEvent();
        }

        if (loadedMode == SaveMode.GAME) {
            resumeTargetState = new BidState(getGame());
        } else {
            resumeTargetState = new CountState(getGame());
        }
        return new PrintNamesEvent(getPlayerNames());
    }

    /**
     * Validates and saves the number of bots
     * 
     * @return GameEvent
     */
    private GameEvent<?> handleBotAmount(GameAction action) {
        int bots = switch (action) {
            case NumberAction(int selected) -> selected;
            default -> -1;
        };
        if (bots < 0 || bots > 4) {
            return new ErrorEvent(0, 4);
        }
        totalBots = bots;
        state = SetupState.ENTER_HUMANS;
        return new PlayerNameEvent(1);
    }

    /**
     * Recursively collects names for human players until the 4-player limit is
     * reached.
     * 
     * @return GameEvent
     */
    private GameEvent<?> handleHumanInput(GameAction action) {
        String name = switch (action) {
            case TextAction(String playerName) -> playerName;
            default -> null;
        };
        if (name == null)
            return new PrintNamesEvent(getPlayerNames());

        getGame().addPlayer(new Player(new HumanStrategy(), name));
        humanCount++;

        if (humanCount < (4 - totalBots)) {
            return new PlayerNameEvent(humanCount + 1);
        }

        if (totalBots > 0) {
            state = SetupState.ENTER_BOTS;
            return new BotStrategyEvent(1);
        }

        return new PrintNamesEvent(getPlayerNames());
    }

    /**
     * Recursively sets strategies for bots.
     * 
     * @return GameEvent
     */
    private GameEvent<?> handleBotInput(GameAction action) {
        int strategy = switch (action) {
            case NumberAction(int selected) -> selected;
            default -> -1;
        };
        if (strategy < 1 || strategy > 2) {
            return new ErrorEvent(1, 2);
        }

        Player bot = strategy == 1
                ? new Player(new HighBotStrategy(), "Bot" + (botCount + 1))
                : new Player(new LowBotStrategy(), "Bot" + (botCount + 1));

        getGame().addPlayer(bot);
        botCount++;

        if (botCount < totalBots) {
            return new BotStrategyEvent(botCount + 1);
        }

        return new PrintNamesEvent(getPlayerNames());
    }

    /**
     * Utility to extract current player names.
     */
    private List<String> getPlayerNames() {
        return getGame().getPlayers().stream().map(Player::getName).toList();
    }

    /**
     * Transitions to BidState for active gameplay or CountState for score
     * simulation.
     * 
     * @return The next State for the GameController.
     */
    @Override
    public State nextState() {
        if (resumeTargetState != null) {
            State next = resumeTargetState;
            resumeTargetState = null;
            return next;
        }
        if (keuze == 1) {
            getGame().setDeck(new Deck());
            getGame().setRandomDealer();
            return new BidState(getGame());
        } else {
            return new CountState(getGame());
        }
    }
}