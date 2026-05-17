package base.commands.actions;

import base.commands.ReversibleAction;
import base.domain.WhistGame;
import base.domain.player.Player;

public class RemovePlayerAction implements ReversibleAction {
    private final WhistGame game;
    private final Player player;
    private final int originalIndex;

    public RemovePlayerAction(WhistGame game, Player player) {
        this.game          = game;
        this.player        = player;
        this.originalIndex = game.getAllPlayers().indexOf(player);
    }

    @Override public void execute() { game.removePlayer(player); }
    @Override public void undo()    { game.addPlayerAtIndex(player, originalIndex); }
}