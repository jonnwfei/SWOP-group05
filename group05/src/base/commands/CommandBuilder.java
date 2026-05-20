package base.commands;

import base.commands.actions.AddPlayerAction;
import base.commands.actions.RemovePlayerAction;
import base.commands.actions.RemoveRoundAction;
import base.domain.WhistGame;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.round.Round;
import base.domain.strategy.HighBotStrategy;
import base.domain.strategy.HumanStrategy;
import base.domain.strategy.LowBotStrategy;
import base.domain.strategy.SmartBotStrategy;

/**
 * Factory for creating {@link ReversibleAction} instances that modify a {@link WhistGame}.
 * All actions produced here are ready to be passed to an {@link base.commands.ActionHistory}
 * for execution with full undo/redo support.
 */
public class CommandBuilder {

    private final WhistGame game;

    /**
     * @param game the game that all produced actions will operate on; must not be {@code null}
     * @throws IllegalArgumentException if {@code game} is {@code null}
     */
    public CommandBuilder(WhistGame game) {
        if (game == null) throw new IllegalArgumentException("game cannot be null");
        this.game = game;
    }

    /**
     * Creates an action that adds a new human player with the given name.
     *
     * @param name the player's display name; must not be {@code null} or blank
     * @throws IllegalArgumentException if {@code name} is {@code null} or blank
     */
    public ReversibleAction addHumanPlayer(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name cannot be null or blank");
        return new AddPlayerAction(game, new Player(new HumanStrategy(), name));
    }

    /**
     * Creates an action that adds a new smart-bot player with the given name.
     *
     * @param name the player's display name; must not be {@code null} or blank
     * @throws IllegalArgumentException if {@code name} is {@code null} or blank
     */
    public ReversibleAction addSmartBot(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name cannot be null or blank");
        PlayerId id = new PlayerId();
        return new AddPlayerAction(game, new Player(new SmartBotStrategy(), name, id));
    }

    /**
     * Creates an action that adds a new high-bot player with the given name.
     *
     * @param name the player's display name; must not be {@code null} or blank
     * @throws IllegalArgumentException if {@code name} is {@code null} or blank
     */
    public ReversibleAction addHighBot(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name cannot be null or blank");
        return new AddPlayerAction(game, new Player(new HighBotStrategy(), name));
    }

    /**
     * Creates an action that adds a new low-bot player with the given name.
     *
     * @param name the player's display name; must not be {@code null} or blank
     * @throws IllegalArgumentException if {@code name} is {@code null} or blank
     */
    public ReversibleAction addLowBot(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name cannot be null or blank");
        return new AddPlayerAction(game, new Player(new LowBotStrategy(), name));
    }

    /**
     * Creates an action that removes the given player from the game.
     *
     * @param player the player to remove; must not be {@code null} and must be in the game
     * @throws IllegalArgumentException if {@code player} is {@code null} or not in the game
     */
    public ReversibleAction removePlayer(Player player) {
        if (player == null) throw new IllegalArgumentException("player cannot be null");
        return new RemovePlayerAction(game, player);
    }

    /**
     * Creates an action that removes the player at the given list index.
     *
     * @param index zero-based index into the game's player list
     * @throws IllegalArgumentException if {@code index} is out of bounds
     */
    public ReversibleAction removePlayerAtIndex(int index) {
        int size = game.getAllPlayers().size();
        if (index < 0 || index >= size)
            throw new IllegalArgumentException("index " + index + " out of bounds for player list of size " + size);
        return new RemovePlayerAction(game, game.getAllPlayers().get(index));
    }

    /**
     * Creates an action that removes the given round from the game.
     *
     * @param round the round to remove; must not be {@code null} and must be in the game
     * @throws IllegalArgumentException if {@code round} is {@code null} or not in the game
     */
    public ReversibleAction removeRound(Round round) {
        if (round == null) throw new IllegalArgumentException("round cannot be null");
        int index = game.getRounds().indexOf(round);
        if (index == -1) throw new IllegalArgumentException("round is not part of this game");
        return new RemoveRoundAction(game, round, index);
    }
}
