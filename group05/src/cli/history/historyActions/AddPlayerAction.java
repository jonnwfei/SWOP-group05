package cli.history.historyActions;

import base.GameController;
import base.domain.WhistGame;
import base.domain.player.Player;
import cli.history.ReversibleAction;

public class AddPlayerAction implements ReversibleAction {
    private final WhistGame game;
    private final Player player;

    public AddPlayerAction(WhistGame game, Player player) {
        this.game   = game;
        this.player = player;
    }

    @Override public void execute() { game.addPlayer(player); }
    @Override public void undo()    { game.removePlayer(player); }
}
