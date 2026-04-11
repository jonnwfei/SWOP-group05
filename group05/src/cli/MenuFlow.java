package cli;

import base.domain.WhistGame;
import base.domain.deck.Deck;
import base.domain.player.*;
import cli.events.IOEvent;
import cli.events.menu.*;

import java.util.List;

public class MenuFlow {

    private final TerminalManager terminalManager;
    private final WhistGame game;

    public MenuFlow(TerminalManager terminalManager, WhistGame game) {
        this.terminalManager = terminalManager;
        this.game = game;
    }

    public void run() {
        game.resetPlayers();
        game.resetRounds();

        int choice = askInt(new WelcomeMenuIOEvent());

        if (choice == 1) setupGame();
        else if (choice == 2) setupCount();
    }

    private void setupGame() {
        int bots = askInt(new AmountOfBotsIOEvent());
        int humans = 4 - bots;

        for (int i = 1; i <= humans; i++) {
            String name = askString(new PlayerNameIOEvent(i));
            game.addPlayer(new Player(new HumanStrategy(), name));
        }

        for (int i = 1; i <= bots; i++) {
            int strategy = askInt(new BotStrategyIOEvent(i));
            Player bot = strategy == 1
                    ? new Player(new HighBotStrategy(), "Bot" + i)
                    : new Player(new LowBotStrategy(), "Bot" + i);
            game.addPlayer(bot);
        }

        game.setDeck(new Deck());
        game.setRandomDealer();

        terminalManager.handle(new PrintNamesIOEvent(
                game.getPlayers().stream().map(Player::getName).toList()
        ));
        game.startGame(); // WhistGame decides its own state
    }

    private void setupCount() {
        for (int i = 1; i <= 4; i++) {
            String name = askString(new PlayerNameIOEvent(i));
            game.addPlayer(new Player(new HumanStrategy(), name));
        }

        terminalManager.handle(new PrintNamesIOEvent(
                game.getPlayers().stream().map(Player::getName).toList()
        ));

        game.startCount();
    }

    private int askInt(IOEvent event) {
        return Integer.parseInt(terminalManager.handle(event).rawInput());
    }

    private String askString(IOEvent event) {
        return terminalManager.handle(event).rawInput();
    }
}