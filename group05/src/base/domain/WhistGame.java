package base.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import base.domain.actions.GameAction;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.deck.Deck;
import base.domain.player.*;
import base.domain.round.Round;
import base.domain.states.*;
import base.domain.events.GameEvent;

/**
 * Represents a game of Whist
 * @author Stan Kestens
 * @since 28/02/2026
 */
public class WhistGame {

    private State state;
    private List<Player> allplayers;
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
        this.allplayers = new ArrayList<>();
        this.players = new ArrayList<>();
        this.rounds = new ArrayList<>();
        this.currentPlayer = null;
        this.dealerPlayer = null;
    }

    /** @return A shallow copy of the list of active players registered in the game. */
    public List<Player> getPlayers(){
        return new ArrayList<>(this.players);
    }

    /** @return A shallow copy of the list of all players registered in the game. */
    public List<Player> getAllPlayers() { return new ArrayList<>(this.allplayers); }

    /**@param players a list of the active players */
    public void setPlayers(List<Player> players){this.players = players; }

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

    /**
     * Creates and initializes the next round, automatically calculating the score multiplier
     * based on whether the previous round was passed.
     * @param startingPlayer The player who gets the first turn.
     * @throws IllegalArgumentException if the starting player is null or not actively in the game.
     * @throws IllegalStateException if the game does not have exactly 4 players.
     */
    public void initializeNextRound(Player startingPlayer) {
        if (startingPlayer == null) {
            throw new IllegalArgumentException("Cannot initialize round: startingPlayer is null.");
        }
        if (!this.players.contains(startingPlayer)) {
            throw new IllegalArgumentException("Cannot initialize round: startingPlayer must be one of the registered players.");
        }
        if (allplayers.size() > 4){
            if (this.rounds.isEmpty()) {
                setPlayers(allplayers.subList(0,4));
            } else {
                int nextplayeridx = allplayers.stream().mapToInt(p -> players.indexOf(p)).max().orElseThrow();
                while (players.contains(allplayers.get(nextplayeridx))){
                    nextplayeridx = (nextplayeridx + 1) % allplayers.size();
                }
                int currentIdxofdealer = players.indexOf(dealerPlayer);
                players.set(currentIdxofdealer, allplayers.get(nextplayeridx));
                this.dealerPlayer = players.get((currentIdxofdealer + 1) % players.size());
            }
        }
        if (this.players == null || this.players.size() != 4) {
            throw new IllegalStateException("Cannot initialize round: The game must have exactly 4 players.");
        }
        int multiplier = 1;
        if (!this.rounds.isEmpty() && getCurrentRound().getHighestBid().getType() == BidType.PASS) {
            multiplier = 2;
        }

        Round newRound = new Round(this.players, startingPlayer, multiplier);
        addRound(newRound);
    }

    /**
     * Deals 13 cards to each player and determines the initial dealt trump suit.
     * @return The originally dealt trump suit (the suit of the last card dealt).
     * @throws IllegalStateException if the deck is not set, if there are not exactly 4 players,
     * or if the deck deals invalid hands.
     */
    public Suit dealCards() {
        if (this.deck == null) {
            throw new IllegalStateException("Cannot deal cards: Deck is not set.");
        }
        if (this.players == null) {
            throw new IllegalStateException("Cannot deal cards: The game cannnot have 0 players players.");
        }
        if (this.players.size() != 4) {
            System.out.println(this.players.size());
            throw new IllegalStateException("Cannot deal cards: The game must have 4 players players.");
        }

        this.deck.shuffle();
        List<List<Card>> hands = this.deck.deal();

        for (int i = 0; i < this.players.size(); i++) {
            this.players.get(i).setHand(hands.get(i));
        }

        // Return the suit of the very last card dealt
        return this.players.getLast().getHand().getLast().suit();
    }

    /** @param deck The deck to be used for the upcoming deal. */
    public void setDeck(Deck deck){
        this.deck = deck;
    }

    /**
     * Triggers a transition to the next game state based on internal logic.
     */
    public void nextState(){
        this.state = state.nextState();
    }

    /**
     * Delegates the provided action to the current state for processing.
     * @param response The user action to process.
     * @return A {@link GameEvent} describing the outcome of the action.
     */
    public GameEvent<?> executeState(GameAction response){
        return state.executeState(response);
    }

    /** @param player The player to add to the game session. */
    public void addPlayer(Player player){
        this.allplayers.add(player);
    }

    /** @param round The round to add to the game history. */
    public void addRound (Round round){
        this.rounds.add(round);
    }

    /** Clears all players from the game. */
    public void resetPlayers(){
        this.allplayers = new ArrayList<>();
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
        if (players.size() != 4) throw new IllegalArgumentException("Must have 4 players");
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