package cli;

import base.domain.WhistGame;
import base.domain.deck.Deck;
import base.domain.player.*;
import base.storage.GamePersistenceService;
import base.storage.snapshots.SaveMode;
import base.domain.strategy.*;
import cli.events.IOEvent;

import static cli.events.MenuEvents.*;

import java.util.List;

public class MenuFlow {

    private final TerminalManager terminalManager;
    private final GamePersistenceService persistenceService;
    private final WhistGame game;
    private SaveMode savedMode;
    public MenuFlow(TerminalManager terminalManager, GamePersistenceService persistenceService, WhistGame game) {
        this.terminalManager = terminalManager;
        this.persistenceService = persistenceService;
        this.game = game;
    }

    public SaveMode run() {
        game.resetPlayers();
        game.resetRounds();

        int choice = askInt(new WelcomeMenuIOEvent(), 1, 3);

        return switch (choice) {
            case 1 -> { setupGame();     yield SaveMode.GAME; }
            case 2 -> { setupCount();   yield SaveMode.COUNT; }
            case 3 -> { setupLoadSave(); yield savedMode; } // see below
            default -> throw new IllegalStateException("Unexpected menu choice: " + choice);
        };
    }


    private void setupGame() {
        int bots = askInt(new AmountOfBotsIOEvent(), 0, 3);
        int humans = 4 - bots;

        for (int i = 1; i <= humans; i++) {
            String name = askString(new PlayerNameIOEvent(i));
            game.addPlayer(new Player(new HumanStrategy(), name, new PlayerId()));
        }

        for (int i = 1; i <= bots; i++) {
            int strategy = askInt(new BotStrategyIOEvent(i), 1, 2);
            Player bot = strategy == 1
                    ? new Player(new HighBotStrategy(), "Bot" + i, new PlayerId())
                    : new Player(new LowBotStrategy(), "Bot" + i, new PlayerId());
            game.addPlayer(bot);
        }

        game.setDeck(new Deck());
        game.setRandomDealer();

        terminalManager.handle(new PrintNamesIOEvent(
                game.getPlayers().stream().map(Player::getName).toList()));
        game.startGame();
    }

    private void setupCount() {
        for (int i = 1; i <= 4; i++) {
            String name = askString(new PlayerNameIOEvent(i));
            game.addPlayer(new Player(new HumanStrategy(), name,  new PlayerId()));
        }

        terminalManager.handle(new PrintNamesIOEvent(
                game.getPlayers().stream().map(Player::getName).toList()));

        game.startCount();
    }

    private void setupLoadSave() {
        List<String> saves = persistenceService.listDescriptions();
        if (saves.isEmpty()) {
            System.out.println("No saved games found. Returning to main menu.");
            savedMode = run(); // recurse and propagate the mode back up
            return;
        }

        int choice = askInt(new LoadSaveIOEvent(saves), 1, saves.size());
        String description = saves.get(choice - 1);

        try {
            SaveMode mode = persistenceService.loadIntoGame(game, description);
            this.savedMode = mode;
            switch (mode) {
                case GAME  -> game.startGame();
                case COUNT -> game.startCount();
            }
        } catch (Exception e) {
            System.out.println("Failed to load save: " + e.getMessage());
            savedMode = run();
        }
    }

    // --- Input Helpers ---

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

    private int askInt(IOEvent event, int min, int max) {
        while (true) {
            int value = askInt(event);
            if (value >= min && value <= max)
                return value;
            System.out.println("Please enter a number between " + min + " and " + max + ".");
        }
    }

    private String askString(IOEvent event) {
        while (true) {
            String raw = terminalManager.handle(event).rawInput();
            if (raw != null && !raw.isBlank())
                return raw.trim();
            System.out.println("Input cannot be empty.");
        }
    }
}