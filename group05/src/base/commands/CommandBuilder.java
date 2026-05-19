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

public class CommandBuilder {

    private final WhistGame game;

    public CommandBuilder(WhistGame game) {
        this.game = game;
    }

    public ReversibleAction addHumanPlayer(String name) {
        return new AddPlayerAction(game, new Player(new HumanStrategy(), name));
    }

    public ReversibleAction addSmartBot(String name) {
        PlayerId id = new PlayerId();
        return new AddPlayerAction(game, new Player(new SmartBotStrategy(id), name, id));
    }

    public ReversibleAction addHighBot(String name) {
        return new AddPlayerAction(game, new Player(new HighBotStrategy(), name));
    }

    public ReversibleAction addLowBot(String name) {
        return new AddPlayerAction(game, new Player(new LowBotStrategy(), name));
    }

    public ReversibleAction removePlayer(Player player) {
        return new RemovePlayerAction(game, player);
    }

    public ReversibleAction removePlayerAtIndex(int index) {
        return new RemovePlayerAction(game, game.getAllPlayers().get(index));
    }

    public ReversibleAction removeRound(Round round) {
        int index = game.getRounds().indexOf(round);
        return new RemoveRoundAction(game, round, index);
    }
}
