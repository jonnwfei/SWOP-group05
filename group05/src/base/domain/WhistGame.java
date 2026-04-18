package base.domain;

import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.commands.GameCommand;
import base.domain.deck.Deck;
import static base.domain.deck.Deck.DEAL_TYPE;

import base.domain.observer.GameObserver;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.round.Round;
import base.domain.states.BidState;
import base.domain.states.CountState;
import base.domain.states.State;
import base.domain.states.StateStep;
import base.domain.turn.BidTurn;
import base.domain.turn.PlayTurn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Represents a game of Whist.
 * Acts as the Aggregate Root, managing the global player roster, round history, and state transitions.
 * @author Stan Kestens, Tommy Wu
 * @since 28/02/2026
 */
public class WhistGame {

    public static final int REQUIRED_PLAYERS = 4;

    private State state;
    private Deck deck;
    private Player dealerPlayer;
    private final List<Player> allPlayers;
    private final List<Round> rounds;
    private final List<GameObserver> observers;

    public WhistGame() {
        this.state = null;
        this.allPlayers = new ArrayList<>();
        this.rounds = new ArrayList<>();
        this.observers = new ArrayList<>();
        this.dealerPlayer = null;
    }

    // --- Player Management ---

    /**
     * Finds the next player in the seating order.
     * Centralizes the rotational logic so State classes don't have to do modulo math.
     */
    public Player getNextPlayer(Player currentPlayer) {
        List<Player> activePlayers = getPlayers();
        if (currentPlayer == null || !activePlayers.contains(currentPlayer)) {
            throw new IllegalArgumentException("Cannot find next player: current player is not at the table.");
        }
        int currentIndex = activePlayers.indexOf(currentPlayer);
        return activePlayers.get((currentIndex + 1) % activePlayers.size());
    }

    /**
     * Maps a PlayerId back to the physical Player object.
     * Centralized here so States and Rounds can operate purely on IDs for security.
     */
    public Player getPlayerById(PlayerId id) {
        if (id == null) throw new IllegalArgumentException("PlayerId cannot be null.");
        return allPlayers.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("PlayerId " + id + " not found in the game."));
    }

    /**
     * Returns the active players currently seated at the table (max 4).
     */
    public List<Player> getPlayers() {
        if (allPlayers.size() < REQUIRED_PLAYERS) {
            throw new IllegalStateException("Active table is not ready (needs 4 players).");
        }
        return List.copyOf(allPlayers.subList(0, REQUIRED_PLAYERS));
    }

    /**
     * Returns all players currently participating in this game.
     */
    public List<Player> getAllPlayers() {
        return List.copyOf(this.allPlayers);
    }

    public int getTotalPlayerCount() {
        return allPlayers.size();
    }

    public List<PlayerId> getPlayerIds() {
        return allPlayers.stream().map(Player::getId).toList();
    }

    public void addPlayer(Player player) {
        if (player == null) throw new IllegalArgumentException("Player cannot be null");
        if (allPlayers.contains(player))
            throw new IllegalArgumentException("Player already in Game");
        this.allPlayers.add(player);
    }

    /**
     * Removes any player from the game roster as long as at least 4 players remain.
     *
     * @param player player to remove
     */
    public void removePlayer(Player player) {
        if (player == null) throw new IllegalArgumentException("Player cannot be null");
        if (getTotalPlayerCount() <= REQUIRED_PLAYERS) {
            throw new IllegalStateException("Cannot remove player: at least 4 total players are required.");
        }

        boolean removed = this.allPlayers.remove(player);
        if (!removed) {
            throw new IllegalArgumentException("Cannot remove player: player is not part of this game.");
        }

        if (player.equals(this.dealerPlayer)) {
            this.dealerPlayer = getPlayers().isEmpty() ? null : getPlayers().getFirst();
        }
    }

    public void resetPlayers() {
        this.allPlayers.clear();
        this.dealerPlayer = null;
    }

    // --- Dealer Management ---

    public Player getDealerPlayer() {
        return this.dealerPlayer;
    }

    public void setDealerPlayer(Player dealer) {
        if (dealer != null && !getPlayers().contains(dealer)) {
            throw new IllegalArgumentException("Dealer must be a player currently in the game.");
        }
        this.dealerPlayer = dealer;
    }

    public void setRandomDealer() {
        List<Player> activePlayers = getPlayers();
        if (activePlayers.isEmpty()) throw new IllegalStateException("Cannot set random dealer: no players in game.");
        this.dealerPlayer = activePlayers.get(new Random().nextInt(activePlayers.size()));
    }

    /**
     * Advances the dealer designation to the next player using our clean roster method.
     */
    public void advanceDealer() {
        if (getPlayers().isEmpty() || dealerPlayer == null) {
            throw new IllegalStateException("Cannot advance dealer: missing players or current dealer.");
        }
        this.dealerPlayer = getNextPlayer(dealerPlayer);
    }

    // --- Round & Game Flow Management ---

    public Deck getDeck() {
        return this.deck;
    }

    public void setDeck(Deck deck) {
        this.deck = deck;
    }

    public List<Round> getRounds() {
        return Collections.unmodifiableList(rounds);
    }

    public void addRound(Round round) {
        if (round == null) throw new IllegalArgumentException("Round cannot be null");
        this.rounds.add(round);
    }

    public void resetRounds() {
        this.rounds.clear();
    }

    public Round getCurrentRound() {
        return rounds.isEmpty() ? null : rounds.getLast();
    }

    /**
     * Determines the winner of the most recent round.
     *
     * @return The winning {@link Player}, or null if no rounds have finished.
     */
    public Player getLastRoundWinner() {
        if (this.rounds.isEmpty())
            return null;
        List<Player> winningPlayers = rounds.getLast().getWinningPlayers();
        if (winningPlayers.isEmpty())
            return null;
        return winningPlayers.getFirst();
    }

    /**
     * Deals 13 cards to each player and determines the initial dealt trump suit.
     *
     * @return The originally dealt trump suit (the suit of the last card dealt).
     * @throws IllegalStateException if the deck is not set, if there are not exactly 4 players,
     * or if the deck deals invalid hands.
     */
    public Suit dealCards() {
        List<Player> activePlayers = getPlayers();
        if (this.deck == null) throw new IllegalStateException("Cannot deal cards: Deck is not set.");
        if (activePlayers.size() != REQUIRED_PLAYERS) throw new IllegalStateException("Cannot deal cards: Table must have exactly 4 players.");

        this.deck.shuffle();
        List<List<Card>> hands = this.deck.deal(DEAL_TYPE.WHIST);

        Suit dealtTrump = hands.getLast().getLast().suit();

        for (int i = 0; i < activePlayers.size(); i++) {
            activePlayers.get(i).setHand(hands.get(i));
        }

        return dealtTrump;
    }

    public void initializeNextRound(Player startingPlayer) {
        List<Player> activePlayers = getPlayers();
        if (activePlayers.size() < REQUIRED_PLAYERS)
            throw new IllegalStateException("Game must have at least 4 players to start a round.");
        if (!activePlayers.contains(startingPlayer))
            throw new IllegalArgumentException("Starting player not at the table.");

        int multiplier = 1;
        if (!this.rounds.isEmpty() && getCurrentRound().getHighestBid().getType() == BidType.PASS) {
            multiplier = 2;
        }

        addRound(new Round(activePlayers, startingPlayer, multiplier));
    }

    // --- State Machine ---

    public void startGame() {
        this.state = new BidState(this);
    }

    public void startCount() {
        this.state = new CountState(this);
    }

    /**
     * Triggers a transition to the next game state based on internal logic.
     */
    public void nextState() {
        this.state = state.nextState();
    }

    /**
     * Executes the current state without a user command (e.g. for bot turns or automatic transitions).
     *
     * @return The resulting event to be rendered by the UI.
     */
    public StateStep executeState() {
        return state.executeState();
    }

    /**
     * Delegates the provided action to the current state for processing.
     *
     * @param command The user action to process.
     * @return The resulting event to be rendered by the UI.
     */
    public StateStep executeState(GameCommand command) {
        return state.executeState(command);
    }

    public boolean isOver() {
        return this.state == null;
    }

    // --- Observer Management ---

    public void addObserver(GameObserver observer) {
        if (observer != null) this.observers.add(observer);
    }

    public List<GameObserver> getObservers() {
        return Collections.unmodifiableList(observers);
    }

    public void notifyRoundStarted() {
        List<PlayerId> ids = getPlayers().stream().map(Player::getId).toList();
        for (GameObserver observer : observers) observer.onRoundStarted(ids);
    }

    public void notifyTrumpDetermined(Suit trumpSuit) {
        for (GameObserver observer : observers) observer.onTrumpDetermined(trumpSuit);
    }

    public void notifyBidPlaced(BidTurn bidTurn) {
        for (GameObserver observer : observers) observer.onBidPlaced(bidTurn);
    }

    public  void removeRound(Round round){
        this.rounds.remove(round);
    }

    /**
     * Rotates seats by moving the current dealer to the end of the global queue.
     */
    public void rotateActivePlayers() { //TODO: make use of this
        if (getTotalPlayerCount() <= REQUIRED_PLAYERS) return;

        int dealerIndex = allPlayers.indexOf(dealerPlayer);
        if (dealerPlayer == null || dealerIndex < 0 || dealerIndex >= REQUIRED_PLAYERS) {
            throw new IllegalStateException("Cannot rotate players: dealer must be one of the active players.");
        }

        Player leavingPlayer = allPlayers.remove(dealerIndex);
        allPlayers.add(leavingPlayer);
    }

    /**
     * Recalculates all player scores from scratch based on the current round history.
     * Call this after removing a round to ensure the scoreboard is accurate.
     */
    public void recalibrateScores() {
        // 1. Reset all players to 0
        for (Player p : this.allPlayers) {
            p.updateScore(-p.getScore());
        }

        List<PlayerId> currentPlayerIds = getPlayerIds();

        // 2. Re-apply deltas from all rounds currently in the list
        for (Round round : this.rounds) {
            List<Integer> deltas = round.getScoreDeltas();
            List<Player> roundPlayers = round.getPlayers();

            // Map the deltas back to the players based on their index in the round
            for (int i = 0; i < roundPlayers.size(); i++) {
                Player historicalPlayer = roundPlayers.get(i);
                if (!currentPlayerIds.contains(historicalPlayer.getId())) {
                    continue;
                }

                int delta = deltas.get(i);
                Player currentPlayer = getPlayerById(historicalPlayer.getId());
                currentPlayer.updateScore(delta);
            }
        }
    }
    public void notifyTurnPlayed(PlayTurn playTurn) {
        for (GameObserver observer : observers) observer.onTurnPlayed(playTurn);
    }
}