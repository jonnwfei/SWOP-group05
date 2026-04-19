package cli.flows;

import base.domain.WhistGame;
import base.domain.WhistRules;
import base.domain.deck.Deck;
import base.domain.player.*;
import base.storage.GamePersistenceService;
import base.storage.snapshots.SaveMode;
import base.domain.strategy.*;
import cli.TerminalManager;
import cli.events.IOEvent;

import static cli.events.MenuEvents.*;

import java.util.List;
/**
 * Handles the main menu of the application.
 * Responsible for setting up a new game, count mode,
 * or loading an existing saved game.
 * @author stankestens
 */
public class MenuFlow {

    private final TerminalManager terminalManager;
    private final GamePersistenceService persistenceService;
    private final WhistGame game;
    private SaveMode savedMode;
    /**
     * Creates a new MenuFlow.
     *
     * @param terminalManager handles user interaction
     * @param persistenceService handles save/load operations
     * @param game the game instance to configure
     */
    public MenuFlow(TerminalManager terminalManager, GamePersistenceService persistenceService, WhistGame game) {
        this.terminalManager = terminalManager;
        this.persistenceService = persistenceService;
        this.game = game;
    }
    /**
     * Displays the main menu and executes the selected option.
     *
     * @return the selected SaveMode (GAME or COUNT)
     */
    public SaveMode run() {
        int choice = askInt(new WelcomeMenuIOEvent(), 1, 3);

        return switch (choice) {
            case 1 -> { setupGame();     yield SaveMode.GAME; }
            case 2 -> { setupCount();   yield SaveMode.COUNT; }
            case 3 -> { setupLoadSave(); yield savedMode; } // see below
            default -> throw new IllegalStateException("Unexpected menu choice: " + choice);
        };
    }

    /**
     * Sets up a full game with humans and bots.
     */
    private void setupGame() {
        int bots = askInt(new AmountOfBotsIOEvent(), 0, WhistRules.REQUIRED_PLAYERS );
        int minHumans = WhistRules.REQUIRED_PLAYERS - bots;
        int humans = askInt(new AmountOfHumansIOEvent(minHumans, WhistRules.MAX_PLAYERS), minHumans, WhistRules.MAX_PLAYERS );

        addHumanPlayers(1, humans);
        addBotPlayers(humans + 1, bots);

        game.setDeck(new Deck());
        game.setRandomDealer();

        printPlayerNames();
        game.startGame();
    }

    /**
     * Sets up count mode with only human players.
     */
    private void setupCount() {
        addHumanPlayers(1, WhistRules.REQUIRED_PLAYERS);
        game.setDealerPlayer(game.getAllPlayers().getFirst()); // default to first player as dealer for counting mode

        printPlayerNames();
        game.startCount();
    }
    /**
     * Loads a saved game and restores its mode.
     */
    private void setupLoadSave() {
        List<String> availableSaves = persistenceService.listDescriptions();
        if (availableSaves.isEmpty()) {
            System.out.println("No saved games found. Returning to main menu.");
            return;
        }

        int saveFileChoice = askInt(new LoadSaveIOEvent(availableSaves), 0, availableSaves.size());
        if (saveFileChoice == 0) return;

        String chosenDescription = availableSaves.get(saveFileChoice - 1); // off by one
        SaveMode saveMode;
        try {
            SaveMode mode = persistenceService.loadIntoGame(game, chosenDescription);
            this.savedMode = mode;
            switch (mode) {
                case GAME  -> game.startGame();
                case COUNT -> game.startCount();
            }
        } catch (Exception e) {
            System.out.println("Error while loading game: " + e);
            throw new IllegalArgumentException("Failed to load game with description: " + chosenDescription, e);
        }
    }


    /**
     * Adds human players to the game.
     *
     * @param startIndex starting player number
     * @param amount number of players to add
     */
    private void addHumanPlayers(int startIndex, int amount) {
        for (int i = 0; i < amount; i++) {
            int playerNumber = startIndex + i;
            String name = askString(new PlayerNameIOEvent(playerNumber));
            // Using the convenience constructor that auto-generates the PlayerId
            game.addPlayer(new Player(new HumanStrategy(), name));
        }
    }
    /**
     * Adds bot players with selected strategies.
     *
     * @param startIndex starting player number
     * @param amount number of bots to add
     */
    private void addBotPlayers(int startIndex, int amount) {
        for (int i = 0; i < amount; i++) {
            int botNumber = startIndex + i;
            int strategy = askInt(new BotStrategyIOEvent(botNumber), 1, 3);

            Player bot = switch (strategy) {
                case 1 -> new Player(new HighBotStrategy(), "Bot " + botNumber);
                case 2 -> new Player(new LowBotStrategy(), "Bot " + botNumber);
                default -> {
                        PlayerId smartId = new PlayerId();
                        yield new Player(new SmartBotStrategy(smartId), "Bot " + botNumber, smartId);
                    }
                };
            game.addPlayer(bot);
        }
    }
    /**
     * Prints all player names to the terminal.
     */
    private void printPlayerNames() {
        terminalManager.handle(new PrintNamesIOEvent(
                game.getAllPlayers().stream().map(Player::getName).toList()
        ));
    }


    /**
     * Reads an integer from the user.
     *
     * @param event IO event to display
     * @return parsed integer
     */
    private int askInt(IOEvent event) {
        while (true) {
            try {
                String raw = terminalManager.handle(event).rawInput();
                return Integer.parseInt(raw.trim());
            } catch (Exception e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }
    /**
     * Reads an integer within a range.
     *
     * @param event IO event to display
     * @param min minimum value
     * @param max maximum value
     * @return validated integer
     */
    private int askInt(IOEvent event, int min, int max) {
        while (true) {
            int value = askInt(event);
            if (value >= min && value <= max)
                return value;
            System.out.println("Please enter a number between " + min + " and " + max + ".");
        }
    }
    /**
     * Reads a non-empty string from the user.
     *
     * @param event IO event to display
     * @return validated string
     */
    private String askString(IOEvent event) {
        while (true) {
            String raw = terminalManager.handle(event).rawInput();
            if (raw != null && !raw.isBlank())
                return raw.trim();
            System.out.println("Input cannot be empty.");
        }
    }
}