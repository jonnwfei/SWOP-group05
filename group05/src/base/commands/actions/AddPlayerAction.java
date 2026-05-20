package base.commands.actions;

import base.commands.ReversibleAction;
import base.domain.WhistGame;
import base.domain.player.Player;

/**
 * Reversible action that adds a {@link Player} to a {@link WhistGame}.
 * Executing adds the player; undoing removes them again.
 */
public class AddPlayerAction implements ReversibleAction {
    private final WhistGame game;
    private final Player player;

    /**
     * @param game   the game to add the player to; must not be {@code null}
     * @param player the player to add; must not be {@code null}
     * @throws IllegalArgumentException if either argument is {@code null}
     */
    public AddPlayerAction(WhistGame game, Player player) {
        if (game   == null) throw new IllegalArgumentException("game cannot be null");
        if (player == null) throw new IllegalArgumentException("player cannot be null");
        this.game   = game;
        this.player = player;
    }

    /** Adds the player to the game. */
    @Override public void execute() { game.addPlayer(player); }

    /** Removes the player from the game, reversing the add. */
    @Override public void undo()    { game.removePlayer(player); }
}