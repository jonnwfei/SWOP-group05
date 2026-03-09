package base.domain;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import base.domain.deck.Deck;
import base.domain.player.*;
import base.domain.round.Round;
import base.domain.states.*;
import base.domain.events.GameEvent;

/**
 * @author Stan Kestens
 * @since 28/02/2026
 */
public class WhistGame {

    private State state;
    private List<Player> players;
    private List<Round> rounds;
    private Player currentPlayer;
    private Player dealerPlayer;
    private Deck deck;

    public WhistGame(){
        this.state = new MenuState(this);
        this.players = new ArrayList<>();
        this.rounds = new ArrayList<>();
        this.currentPlayer = null;
        this.dealerPlayer = null;
    }

    /**
     * Gets a shallow copy of the list of Players
     *
     * @return list of Player
     */
    public List<Player> getPlayers(){
        return new ArrayList<>(this.players);
    }

    /**
     * Gives the deck of the game
     * @return the deck
     */
    public Deck getDeck(){
        return this.deck;
    }

    /**
     * Gets a shallow copy of the list of Rounds
     *
     * @return list of Rounds
     */
    public List<Round> getRounds(){
        return new ArrayList<>(this.rounds);
    }

    /**
     * Gets the active player of Game
     *
     * @return currentPlayer of Game
     */
    public Player getCurrentPlayer(){
        return this.currentPlayer;
    }

    /**
     * Gets the dealer of this Game
     *
     * @return dealerPlayer of this Game
     */
    public Player getDealerPlayer(){
        return this.dealerPlayer;
    }

    /**
     * Gets the winner from the last Round, if no Rounds exist, return null
     *
     * @return Last Round's Winner
     */
    public Player getLastRoundWinner() {
        if (this.rounds.isEmpty()) return null;
        List<Player> winningPlayers = rounds.getLast().getWinningPlayers();
        if (winningPlayers.isEmpty()) return null;

        return winningPlayers.getFirst();
    }
    /**
     * Gets the active round of the Game
     *
     * @return current round or null if no rounds have been played yet
     * */
    public Round getCurrentRound(){
        if (rounds.isEmpty()) return null;
        return this.rounds.getLast();
    }

    public void setDeck(Deck deck){
        this.deck = deck;
    }

    public void nextState(){
        this.state = state.nextState();
    }

    public GameEvent executeState(String response){
        return state.executeState(response);
    }

    /**
     * Adds given player to this Game, players
     *
     * @param player to add
     */
    public void addPlayer(Player player){
        this.players.add(player);
    }
    /**
     * Adds given round to this Game, rounds
     *
     * @param round to add
     */
    public void addRound (Round round){
        this.rounds.add(round);
    }

    /**
     * resets the players
     */
    public void resetPlayers(){
        this.players = new ArrayList<>();
    }

    /**
     * resets the rounds
     */
    public void resetRounds(){
        this.rounds = new ArrayList<>();
    }
    /**
     * Returns a formatted string of the players in this game as follows:
     * <pre>
     * Players in this game:
     * 1. player1
     * 2. player2
     * etc.
     * </pre>
     *
     * @return the formatted player names
     */
    public String printNames() {
        String result = "Players in this game:\n";
        for (int i = 0; i < players.size(); i++) {
            result += (i + 1) + ". " + players.get(i).getName() + "\n";
        }
        return result;
    }

    /**
     * Sets the current player.
     *
     * @param player the player that should become the current player
     * @throws IllegalArgumentException if the player is null or not part of the game
     * @throws IllegalStateException if the players list is not initialized
     */
    public void setCurrentPlayer(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }

        if (players == null) {
            throw new IllegalStateException("Players list is not initialized");
        }

        if (!players.contains(player)) {
            throw new IllegalArgumentException("Player must be part of the players list");
        }

        this.currentPlayer = player;
    }

    /**
     * Helper function that sets the dealerPlayer randomly, only called upon first Round
     * @throws IllegalArgumentException when trying to set a randomDealer when no player list has been initialized
     */
    public void setRandomDealer() {
        if (players.isEmpty()) throw new IllegalArgumentException("Cannot set randomDealer, Players list is not initialized");
        int randIdx = new Random().nextInt(players.size());
        this.dealerPlayer = players.get(randIdx);
    }

    /**
     * Helper function that advances the dealer by one player
     * @throws IllegalArgumentException when trying to advance the dealer when no player list has been initialized or
     * dealer is null
     */
    public void advanceDealer() {
        if (players.isEmpty() || dealerPlayer == null) throw new IllegalStateException("Cannot advanceDealer, " +
                "Players list is not initialized or dealer is null");
        int currentIdx =  players.indexOf(dealerPlayer);
        this.dealerPlayer = players.get((currentIdx + 1)% players.size());
    }
    /**
     * Returns a formatted string of the players and their current score
     * */
    public String printScore(){
        String result = "============== SCORES ==============\n";
        for (Player p : players) {
            result += p.getName() + ": " + p.getScore() + " points\n";
        }
        result += "====================================";
        return result;
    }


}
