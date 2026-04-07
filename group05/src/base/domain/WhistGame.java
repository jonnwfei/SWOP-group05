package base.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import base.domain.actions.GameAction;
import base.domain.commands.GameCommand;
import base.domain.deck.Deck;
import base.domain.player.*;
import base.domain.results.GameResult;
import base.domain.round.Round;
import base.domain.states.*;
import base.domain.events.GameEvent;

/**
 * Represents a game of Whist
 * * @author Stan Kestens
 * @since 28/02/2026
 */
public class WhistGame {

    private State state;
    private List<Player> players;
    private List<Round> rounds;
    private Player currentPlayer;
    private Player dealerPlayer;
    private Deck deck;

    /**
     * Initializes a new game session starting in the {@link MenuState}.
     */
    public WhistGame(){
        this.state = new MenuState(this);
        this.players = new ArrayList<>();
        this.rounds = new ArrayList<>();
        this.currentPlayer = null;
        this.dealerPlayer = null;
    }

    /** @return A shallow copy of the list of players registered in the game. */
    public List<Player> getPlayers(){
        return new ArrayList<>(this.players);
    }

    /** @return The current deck being used for dealing. */
    public Deck getDeck(){
        return this.deck;
    }

    /** @return A shallow copy of all completed and active rounds. */
    public List<Round> getRounds(){
        return new ArrayList<>(this.rounds);
    }

    /** @return The player whose turn it currently is. */
    public Player getCurrentPlayer(){
        return this.currentPlayer;
    }

    /** @return The player currently designated as the dealer. */
    public Player getDealerPlayer(){
        return this.dealerPlayer;
    }

    /**
     * Determines the winner of the most recent round.
     * @return The winning {@link Player}, or null if no rounds have finished.
     */
    public Player getLastRoundWinner() {
        if (this.rounds.isEmpty()) return null;
        List<Player> winningPlayers = rounds.getLast().getWinningPlayers();
        if (winningPlayers.isEmpty()) return null;
        return winningPlayers.getFirst();
    }

    /**
     * Retrieves the round currently being played.
     * @return The active round, or null of there is non
     */
    public Round getCurrentRound(){
        if (rounds.isEmpty()) return null;
        return this.rounds.getLast();
    }

    /** @param deck The deck to be used for the upcoming deal. */
    public void setDeck(Deck deck){
        this.deck = deck;
    }

    /**
     * Restores the dealer from persisted data.
     *
     * @param dealer The dealer player, or null for count sessions.
     */
    public void setDealerPlayer(Player dealer) {
        if (dealer == null) {
            this.dealerPlayer = null;
            return;
        }
        if (!players.contains(dealer)) {
            throw new IllegalArgumentException("Dealer not in game");
        }
        this.dealerPlayer = dealer;
    }

    /**
     * Triggers a transition to the next game state based on internal logic.
     */
    public void nextState(){
        this.state = state.nextState();
    }

    /**
     * Delegates the provided action to the current state for processing.
     * @param command The user action to process.
     * @return A {@link GameEvent} describing the outcome of the action.
     */
    public GameResult executeState(GameCommand command){
        return state.executeState(command);
    }

    /** @param player The player to add to the game session. */
    public void addPlayer(Player player){
        this.players.add(player);
    }

    /** @param round The round to add to the game history. */
    public void addRound (Round round){
        this.rounds.add(round);
    }

    /** Clears all players from the game. */
    public void resetPlayers(){
        this.players = new ArrayList<>();
    }

    /** Clears the round history. */
    public void resetRounds(){
        this.rounds = new ArrayList<>();
    }

    /**
     * Sets the active player.
     * @param player The player to set as current.
     * @throws IllegalArgumentException if player is null or not in the game.
     * @throws IllegalStateException if players list is not initialized.
     */
    public void setCurrentPlayer(Player player) {
        if (player == null) throw new IllegalArgumentException("Player cannot be null");
        if (players == null) throw new IllegalStateException("Players list is not initialized");
        if (!players.contains(player)) throw new IllegalArgumentException("Player not in game");

        this.currentPlayer = player;
    }

    /**
     * Randomly selects a dealer from the player list. Used for the initial round.
     * @throws IllegalArgumentException if no players have been added.
     */
    public void setRandomDealer() {
        if (players.isEmpty()) throw new IllegalArgumentException("Players list is empty");
        int randIdx = new Random().nextInt(players.size());
        this.dealerPlayer = players.get(randIdx);
    }

    /**
     * Advances the dealer designation to the next player in the rotation.
     * @throws IllegalStateException if players are missing or no dealer is set.
     */
    public void advanceDealer() {
        if (players.isEmpty() || dealerPlayer == null) {
            throw new IllegalStateException("Cannot advance dealer: missing players or current dealer.");
        }
        int currentIdx = players.indexOf(dealerPlayer);
        this.dealerPlayer = players.get((currentIdx + 1) % players.size());
    }
}