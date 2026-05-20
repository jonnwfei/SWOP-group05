package cli.flows;

import base.GameController;
import base.domain.WhistRules;
import base.domain.deck.Deck;
import base.storage.GamePersistenceService;
import base.domain.snapshots.SaveMode;
import cli.util.TerminalInputHelper;
import cli.TerminalManager;

import static cli.events.MenuEvents.*;

import java.util.List;

public class MenuFlow {

    private final TerminalInputHelper input;
    private final TerminalManager terminalManager;
    private final GameController controller;
    private final GamePersistenceService persistenceService;
    private SaveMode savedMode;

    public MenuFlow(TerminalManager terminalManager, GamePersistenceService persistenceService, GameController controller) {
        this.input = new TerminalInputHelper(terminalManager);
        this.terminalManager = terminalManager;
        this.controller = controller;
        this.persistenceService = persistenceService;
    }

    public SaveMode run() {
        int choice = input.askInt(new WelcomeMenuIOEvent(), 1, 3);
        return switch (choice) {
            case 1 -> { setupGame();     yield SaveMode.GAME; }
            case 2 -> { setupCount();    yield SaveMode.COUNT; }
            case 3 -> { setupLoadSave(); yield savedMode; }
            default -> throw new IllegalStateException("Unexpected menu choice: " + choice);
        };
    }

    private void setupGame() {
        int bots = input.askInt(new AmountOfBotsIOEvent(), 0, WhistRules.REQUIRED_PLAYERS);
        int minHumans = WhistRules.REQUIRED_PLAYERS - bots;
        int humans = input.askInt(new AmountOfHumansIOEvent(minHumans, WhistRules.MAX_PLAYERS), minHumans, WhistRules.MAX_PLAYERS);

        addHumanPlayers(1, humans);
        addBotPlayers(humans + 1, bots);

        controller.setDeck(new Deck());
        controller.setRandomDealer();
        printPlayerNames();
        controller.startGame();
    }

    private void setupCount() {
        addHumanPlayers(1, WhistRules.REQUIRED_PLAYERS);
        controller.setFirstPlayerAsDealer();
        printPlayerNames();
        controller.startCount();
    }

    private void setupLoadSave() {
        List<String> availableSaves = persistenceService.listDescriptions();
        if (availableSaves.isEmpty()) {
            System.out.println("No saved games found. Returning to main menu.");
//            run();
            return;
        }
        int choice = input.askInt(new LoadSaveIOEvent(availableSaves), 0, availableSaves.size());
        if (choice == 0) return;

        String description = availableSaves.get(choice - 1);
        try {
            SaveMode mode = persistenceService.loadIntoGame(controller.getGame(), description);
            this.savedMode = mode;
            switch (mode) {
                case GAME  -> controller.startGame();
                case COUNT -> controller.startCount();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to load game: " + description, e);
        }
    }

    private void addHumanPlayers(int startIndex, int amount) {
        for (int i = 0; i < amount; i++) {
            String name = input.askString(new PlayerNameIOEvent(startIndex + i));
            controller.addHumanPlayer(name);
        }
    }

    private void addBotPlayers(int startIndex, int amount) {
        for (int i = 0; i < amount; i++) {
            int botNumber = startIndex + i;
            int strategy = input.askInt(new BotStrategyIOEvent(botNumber), 1, 3);
            switch (strategy) {
                case 1 -> controller.addHighBot("Bot " + botNumber);
                case 2 -> controller.addLowBot("Bot " + botNumber);
                default -> controller.addSmartBot("Bot " + botNumber);
            }
        }
    }

    private void printPlayerNames() {
        terminalManager.handle(new PrintNamesIOEvent(controller.getPlayerNames()));
    }
}