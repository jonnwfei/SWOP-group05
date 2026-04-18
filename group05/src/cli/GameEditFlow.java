package cli;

import base.domain.WhistGame;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.round.Round;
import base.domain.strategy.HighBotStrategy;
import base.domain.strategy.HumanStrategy;
import base.domain.strategy.LowBotStrategy;
import base.domain.strategy.SmartBotStrategy;
import base.storage.GamePersistenceService;
import base.storage.snapshots.SaveMode;
import cli.events.IOEvent;

import static cli.events.CountEvents.*;
import static cli.events.MenuEvents.*;

import java.util.Arrays;
import java.util.List;

/**
 * Shared IO flow for cross-cutting game editing actions.
 * <p>
 * Reused by any higher-level flow that wants to offer save / add-player /
 * remove-player / remove-round. Each public method is a single, self-contained
 * user interaction; composition is the caller's responsibility.
 *
 * @author John Cai
 * @since 18/04/2026
 */
public class GameEditFlow {

    private final TerminalManager terminalManager;
    private final WhistGame game;
    private final GamePersistenceService persistenceService;

    public GameEditFlow(TerminalManager terminalManager,
                        WhistGame game,
                        GamePersistenceService persistenceService) {
        if (terminalManager == null)
            throw new IllegalArgumentException("terminalManager cannot be null");
        if (game == null)
            throw new IllegalArgumentException("game cannot be null");
        if (persistenceService == null)
            throw new IllegalArgumentException("persistenceService cannot be null");
        this.terminalManager = terminalManager;
        this.game = game;
        this.persistenceService = persistenceService;
    }

    // =========================================================================
    // SAVE
    // =========================================================================

    /**
     * Prompts the user for a save description and persists the game.
     * Save failures are printed to the terminal; game state is unaffected.
     */
    public void saveGame(SaveMode mode) {
        if (mode == null) throw new IllegalArgumentException("mode cannot be null");
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
     * Full add-player wizard: user picks human / smart bot / high bot / low bot.
     */
    public void addPlayer() {
        int type = askInt(new AddPlayerIOEvent(), 1, 4);
        switch (type) {
            case 1 -> addHumanPlayer();
            case 2 -> addSmartBot();
            case 3 -> addHighBot();
            case 4 -> addLowBot();
        }
    }

    /**
     * Human-only shortcut. Used by count-mode where bots aren't allowed.
     */
    public void addHumanPlayer() {
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

    /**
     * Removes one player selected by the user, but only if the game still has
     * more than four players afterwards.
     *
     * @return true if a player was removed, false otherwise
     */
    public boolean removePlayer() {
        if (!game.canRemovePlayer()) return false;

        List<Integer> indices = askIntList(new PlayerSelectionIOEvent(game.getPlayers(), false, null));
        if (indices.isEmpty()) return false;

        int idx = indices.getFirst();
        if (idx < 1 || idx > game.getPlayers().size()) return false;

        game.removePlayer(game.getPlayers().get(idx - 1));
        return true;
    }

    // =========================================================================
    // REMOVE ROUND
    // =========================================================================

    /**
     * Removes one round selected by the user and recalibrates all scores.
     *
     * @return true if a round was removed, false otherwise
     */
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
    // Input helpers — same style as MenuFlow
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