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

    public MenuFlow(TerminalManager terminalManager, GamePersistenceService persistenceService, WhistGame game) {
        this.terminalManager = terminalManager;
        this.persistenceService = persistenceService;
        this.game = game;
    }

    public void run() {
        game.resetPlayers();
        game.resetRounds();

        int choice = askInt(new WelcomeMenuIOEvent(), 1, 3);

        if (choice == 1)
            setupGame();
        else if (choice == 2)
            setupCount();
        else if (choice == 3)
            setupLoadSave();

    }

    private void setupGame() {
        int bots = askInt(new AmountOfBotsIOEvent(), 0, 3);
        int humans = 4 - bots;

        for (int i = 1; i <= humans; i++) {
            String name = askString(new PlayerNameIOEvent(i));
            game.addPlayer(new Player(new HumanStrategy(), name));
        }

        for (int i = 1; i <= bots; i++) {
            int strategy = askInt(new BotStrategyIOEvent(i), 1, 2);
            Player bot = strategy == 1
                    ? new Player(new HighBotStrategy(), "Bot" + i)
                    : new Player(new LowBotStrategy(), "Bot" + i);
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
            game.addPlayer(new Player(new HumanStrategy(), name));
        }

        terminalManager.handle(new PrintNamesIOEvent(
                game.getPlayers().stream().map(Player::getName).toList()));

        game.startCount();
    }

    private void setupLoadSave() {
        List<String> availableSaves = persistenceService.listDescriptions();
        if (availableSaves.isEmpty()) {
            System.out.println("No saved games found. Returning to main menu.");
            run(); // re-run the menu flow to show the main menu again
        }

        int saveFileChoice = askInt(new LoadSaveIOEvent(availableSaves), 1, availableSaves.size());
        String chosenDescription = availableSaves.get(saveFileChoice - 1); // off by one
        SaveMode saveMode;
        try {
            saveMode = persistenceService.loadIntoGame(game, chosenDescription);
            switch (saveMode) {
                case GAME -> game.startGame();
                case COUNT -> game.startCount();
            }
        } catch (Exception e) {
            System.out.println("Error while loading game: " + e);
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