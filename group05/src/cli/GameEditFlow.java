package cli;

import base.domain.WhistGame;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.round.Round;
import base.domain.strategy.*;
import base.storage.GamePersistenceService;
import base.storage.snapshots.SaveMode;
import cli.events.IOEvent;

import static cli.events.CountEvents.*;
import static cli.events.MenuEvents.*;

import java.util.Arrays;
import java.util.List;

/**
 * Shared IO flow for cross-cutting game editing actions.
 * Behaviour differs between COUNT and GAME mode — e.g. only GAME mode
 * allows adding bots.
 *
 * @author John Cai
 * @since 18/04/2026
 */
public class GameEditFlow {

    private final TerminalManager terminalManager;
    private final WhistGame game;
    private final GamePersistenceService persistenceService;
    private final SaveMode mode;   // COUNT or GAME — governs what actions are available

    public GameEditFlow(TerminalManager terminalManager,
                        WhistGame game,
                        GamePersistenceService persistenceService,
                        SaveMode mode) {
        if (terminalManager == null) throw new IllegalArgumentException("terminalManager cannot be null");
        if (game == null)            throw new IllegalArgumentException("game cannot be null");
        if (persistenceService == null) throw new IllegalArgumentException("persistenceService cannot be null");
        if (mode == null)            throw new IllegalArgumentException("mode cannot be null");
        this.terminalManager   = terminalManager;
        this.game              = game;
        this.persistenceService = persistenceService;
        this.mode              = mode;
    }

    // =========================================================================
    // SAVE
    // =========================================================================

    public void saveGame() {
        String description = askNonBlankString(new SaveDescriptionIOEvent());
        try {
            persistenceService.save(game, mode, description);
        } catch (RuntimeException e) {
            System.out.println("Save failed: " + e.getMessage());
        }
    }

    // =========================================================================
    // ADD PLAYER
    // =========================================================================

    /**
     * Adds a player. In COUNT mode only humans are allowed.
     * In GAME mode the user additionally picks from bot strategies.
     */
    public void addPlayer() {
        if (mode == SaveMode.COUNT) {
            addHumanPlayer();
            return;
        }

        // GAME mode: human or one of three bot types
        int type = askInt(new AddPlayerIOEvent(), 1, 4);
        switch (type) {
            case 1 -> addHumanPlayer();
            case 2 -> addSmartBot();
            case 3 -> addHighBot();
            case 4 -> addLowBot();
        }
    }

    private void addHumanPlayer() {
        String name = askNonBlankString(new AddHumanPlayerIOEvent());
        game.addPlayer(new Player(new HumanStrategy(), name));
    }

    private void addSmartBot() {
        PlayerId id = new PlayerId();
        game.addPlayer(new Player(new SmartBotStrategy(id), "Smart bot", id));
    }

    private void addHighBot() {
        game.addPlayer(new Player(new HighBotStrategy(), "High bot"));
    }

    private void addLowBot() {
        game.addPlayer(new Player(new LowBotStrategy(), "Low bot"));
    }

    // =========================================================================
    // REMOVE PLAYER
    // =========================================================================

    public boolean removePlayer() {
        if (!game.canRemovePlayer()) return false;

        List<Integer> indices = askIntList(new PlayerSelectionIOEvent(game.getAllPlayers(), false, null));
        if (indices.isEmpty()) return false;

        int idx = indices.getFirst();
        if (idx < 1 || idx > game.getAllPlayers().size()) return false;

        game.removePlayer(game.getAllPlayers().get(idx - 1));
        return true;
    }

    // =========================================================================
    // REMOVE ROUND
    // =========================================================================

    public boolean removeRound() {
        List<Round> rounds = game.getRounds();
        if (rounds.isEmpty()) return false;

        int choice = askInt(new DeleteRoundIOEvent(rounds), 0, rounds.size());
        if (choice == 0) return false;

        game.removeRound(rounds.get(choice - 1));
        game.recalibrateScores();
        return true;
    }

    // =========================================================================
    // Input helpers
    // =========================================================================

    private int askInt(IOEvent event, int min, int max) {
        while (true) {
            try {
                String raw = terminalManager.handle(event).rawInput();
                int value = Integer.parseInt(raw.trim());
                if (value >= min && value <= max) return value;
                System.out.println("Please enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    private String askNonBlankString(IOEvent event) {
        while (true) {
            String raw = terminalManager.handle(event).rawInput();
            String value = raw == null ? "" : raw.trim();
            if (!value.isBlank()) return value;
            System.out.println("Input cannot be empty.");
        }
    }

    private List<Integer> askIntList(IOEvent event) {
        while (true) {
            try {
                String raw = terminalManager.handle(event).rawInput();
                if (raw == null || raw.isBlank()) return List.of();
                return Arrays.stream(raw.trim().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Integer::parseInt)
                        .toList();
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter comma-separated numbers.");
            }
        }
    }
}