package base.commands.actions;

import base.commands.ReversibleAction;
import base.domain.WhistGame;
import base.domain.player.Player;

/**
 * Reversible action that removes a {@link Player} from a {@link WhistGame}.
 * The player's original position is remembered so the undo can restore them
 * to the exact same slot in the player list.
 */
public class RemovePlayerAction implements ReversibleAction {
    private final WhistGame game;
    private final Player player;
    private final int originalIndex;

    /**
     * @param game   the game to remove the player from; must not be {@code null}
     * @param player the player to remove; must be present in the game
     * @throws IllegalArgumentException if either argument is {@code null}, or if
     *                                  {@code player} is not part of {@code game}
     */
    public RemovePlayerAction(WhistGame game, Player player) {
        if (game   == null) throw new IllegalArgumentException("game cannot be null");
        if (player == null) throw new IllegalArgumentException("player cannot be null");
        this.game          = game;
        this.player        = player;
        this.originalIndex = game.getAllPlayers().indexOf(player);
        if (this.originalIndex == -1)
            throw new IllegalArgumentException("player is not part of this game");
    }

    /** Removes the player from the game. */
    @Override public void execute() { game.removePlayer(player); }

    /** Re-inserts the player at their original position, reversing the removal. */
    @Override public void undo()    { game.addPlayerAtIndex(player, originalIndex); }
}