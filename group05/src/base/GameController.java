package base;
import base.domain.WhistGame;
import base.domain.commands.GameCommand;
import base.domain.deck.Deck;
import base.domain.observer.GameObserver;
import base.domain.player.Player;
import base.domain.results.GameResult;
import base.domain.round.Round;

import java.util.List;

public class GameController {
    private final WhistGame game;

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
     */
    public WhistGame getGame() {
        return this.game;
    }

    public void setDealerPlayer(Player first) {
        game.setDealerPlayer(first);
    }

    public void setFirstPlayerAsDealer() {
        game.setFirstPlayerAsDealer();
    }

    public void removePlayerAtIndex(int index) {
        game.deletePlayerAtIndex(index);
    }

    public List<String> getPlayerNames() {
        return game.getAllPlayers().stream().map(Player::getName).toList();
    }

    public List<Integer> getPlayerScores() {
        return game.getAllPlayers().stream().map(Player::getScore).toList();
    }

    public int getPlayerCount() {
        return game.getAllPlayers().size();
    }

    public void undo()           { game.undo(); }
    public void redo()           { game.redo(); }
    public boolean canUndo()     { return game.canUndo(); }
    public boolean canRedo()     { return game.canRedo(); }

    public void addRoundAtIndex(Round round, int index) {
        game.addRoundAtIndex(round, index);
    }

    public void addHumanPlayer(String name)  { game.addHumanPlayer(name); }
    public void addSmartBot(String name)     { game.addSmartBot(name); }
    public void addHighBot(String name)      { game.addHighBot(name); }
    public void addLowBot(String name)       { game.addLowBot(name); }

    public void removePlayer(Player player)  { game.deletePlayer(player); }
    public void removeRound(Round round)     { game.deleteRound(round); }

    public void clearHistory()               { game.clearHistory(); }
}
