package base;
import base.domain.WhistGame;
import base.domain.commands.GameCommand;
import base.domain.deck.Deck;
import base.domain.observer.GameObserver;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.results.GameResult;
import base.domain.round.Round;
import base.domain.strategy.HighBotStrategy;
import base.domain.strategy.HumanStrategy;
import base.domain.strategy.LowBotStrategy;
import base.domain.strategy.SmartBotStrategy;
import cli.history.ActionHistory;
import cli.history.historyActions.AddPlayerAction;
import cli.history.historyActions.RemovePlayerAction;
import cli.history.historyActions.RemoveRoundAction;

import java.util.Collection;
import java.util.List;

public class GameController {
    private final WhistGame game;
    private final ActionHistory history = new ActionHistory();

    public GameController(WhistGame game) {
        this.game = game;
    }

    public GameResult advance(GameCommand command) {
        return game.advance(command);
    }

    public void reset() {
        game.resetPlayers();
        game.resetRounds();
    }

    public boolean isGameOver() {
        return game.isOver();
    }

    public List<Player> getPlayers() {
        return game.getAllPlayers();
    }

    public boolean canRemovePlayer() {
        return game.canRemovePlayer();
    }

    public void setDeck(Deck deck) {
        game.setDeck(deck);
    }

    public void setRandomDealer() {
        game.setRandomDealer();
    }

    public void advanceDealer() {
        game.advanceDealer();
    }

    public void rotateActivePlayers() {
        game.rotateActivePlayers();
    }

    public List<Round> getRounds() {
        return game.getRounds();
    }


    public void recalibrateScores() {
        game.recalibrateScores();
    }

    public void startGame() {
        game.startGame();
    }

    public void startCount() {
        game.startCount();
    }

    public void addObserver(GameObserver observer) {
        game.addObserver(observer);
    }

    public List<Player> getAllPlayers() {
        return game.getAllPlayers();
    }

    /**
     * TODO : delete this -> need to fix the uses for the game persistence
     * @return whistgame
     */
    public WhistGame getGame() {
        return this.game;
    }

    public void setDealerPlayer(Player first) {
        game.setDealerPlayer(first);
    }

    // Player factories — flows pass intent, controller constructs


    public void setFirstPlayerAsDealer() {
        game.setFirstPlayerAsDealer();
    }
    // Fix 2: delegate to the history-aware removePlayer
    public void removePlayerAtIndex(int index) {
        Player player = game.getAllPlayers().get(index);
        removePlayer(player); // removePlayer() already calls history.execute(new RemovePlayerAction(...))
    }

    // Projections — flows never need to import Player
    public List<String> getPlayerNames() {
        return game.getAllPlayers().stream().map(Player::getName).toList();
    }

    public List<Integer> getPlayerScores() {
        return game.getAllPlayers().stream().map(Player::getScore).toList();
    }

    public int getPlayerCount() {
        return game.getAllPlayers().size();
    }
    public void undo() { history.undo(); }
    public void redo() { history.redo(); }
    public boolean canUndo() { return history.canUndo(); }
    public boolean canRedo() { return history.canRedo(); }
    public void addRoundAtIndex(Round round, int index) {
        game.addRoundAtIndex(round, index);
    }

    public void addHumanPlayer(String name) {
        Player player = new Player(new HumanStrategy(), name);
        history.execute(new AddPlayerAction(game, player));
    }

    public void addSmartBot(String name) {
        PlayerId id = new PlayerId();
        history.execute(new AddPlayerAction(game, new Player(new SmartBotStrategy(id), name, id)));
    }

    public void addHighBot(String name) {
        history.execute(new AddPlayerAction(game, new Player(new HighBotStrategy(), name)));
    }

    public void addLowBot(String name) {
        history.execute(new AddPlayerAction(game, new Player(new LowBotStrategy(), name)));
    }

    public void removePlayer(Player player) {
        history.execute(new RemovePlayerAction(game, player));
    }

    public void removeRound(Round round) {
        int index = game.getRounds().indexOf(round);
        history.execute(new RemoveRoundAction(game, round, index));
    }

    public void clearHistory() { history.clear(); }
}