package base.domain;

import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.commands.GameCommand;
import base.domain.deck.Deck;
import base.domain.observer.GameEventPublisher;
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
public class WhistGame implements GameEventPublisher {

    private State state;
    private Deck deck;
    private Player dealerPlayer;
    private final List<Player> players;
    private final List<Round> rounds;
    private final List<GameObserver> observers;

    public WhistGame() {
        this.state = null;
        this.players = new ArrayList<>();
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
        if (currentPlayer == null || !players.contains(currentPlayer)) {
            throw new IllegalArgumentException("Cannot find next player: current player is not at the table.");
        }
        int currentIndex = players.indexOf(currentPlayer);
        return players.get((currentIndex + 1) % players.size());
    }

    /**
     * Maps a PlayerId back to the physical Player object.
     * Centralized here so States and Rounds can operate purely on IDs for security.
     */
    public Player getPlayerById(PlayerId id) {
        if (id == null) throw new IllegalArgumentException("PlayerId cannot be null.");
        return players.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("PlayerId " + id + " not found in the game."));
    }

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public List<PlayerId> getPlayerIds() {
        return players.stream().map(Player::getId).toList();
    }

    public void addPlayer(Player player) {
        if (player == null) throw new IllegalArgumentException("Player cannot be null");
        if (players.contains(player)) throw new IllegalArgumentException("Player already in Game");
        this.players.add(player);
        player.getDecisionStrategy().onJoinGame(this);
    }

    public void resetPlayers() {
        this.players.clear();
    }

    // --- Dealer Management ---

    public Player getDealerPlayer() {
        return this.dealerPlayer;
    }

    public void setDealerPlayer(Player dealer) {
        if (dealer != null && !players.contains(dealer)) {
            throw new IllegalArgumentException("Dealer must be a player currently in the game.");
        }
        this.dealerPlayer = dealer;
    }

    public void setRandomDealer() {
        if (players.isEmpty()) throw new IllegalStateException("Cannot set random dealer: no players in game.");
        this.dealerPlayer = players.get(new Random().nextInt(players.size()));
    }

    /**
     * Advances the dealer designation to the next player using our clean roster method.
     */
    public void advanceDealer() {
        if (players.isEmpty() || dealerPlayer == null) {
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
        if (this.deck == null) throw new IllegalStateException("Cannot deal cards: Deck is not set.");
        if (this.players.size() != 4) throw new IllegalStateException("Cannot deal cards: Table must have exactly 4 players.");

        this.deck.shuffle();
        List<List<Card>> hands = this.deck.deal();

        Suit dealtTrump = hands.getLast().getLast().suit();

        for (int i = 0; i < this.players.size(); i++) {
            this.players.get(i).setHand(hands.get(i));
        }

        return dealtTrump;
    }

    public void initializeNextRound(Player startingPlayer) {
        if (this.players.size() != 4) throw new IllegalStateException("Game must have exactly 4 players to start a round.");
        if (!this.players.contains(startingPlayer)) throw new IllegalArgumentException("Starting player not at the table.");

        int multiplier = 1;
        if (!this.rounds.isEmpty() && getCurrentRound().getHighestBid().getType() == BidType.PASS) {
            multiplier = 2;
        }

        addRound(new Round(this.players, startingPlayer, multiplier));
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

    public void notifyRoundStarted() {
        List<PlayerId> ids = players.stream().map(Player::getId).toList();
        for (GameObserver observer : observers) observer.onRoundStarted(ids);
    }

    public void notifyTrumpDetermined(Suit trumpSuit) {
        for (GameObserver observer : observers) observer.onTrumpDetermined(trumpSuit);
    }

    public void notifyBidPlaced(BidTurn bidTurn) {
        for (GameObserver observer : observers) observer.onBidPlaced(bidTurn);
    }

    public void removePlayer(Player player) {
        this.players.remove(player);
    }
    public  void removeRound(Round round){
        this.rounds.remove(round);
    }
    /**
     * Recalculates all player scores from scratch based on the current round history.
     * Call this after removing a round to ensure the scoreboard is accurate.
     */
    public void recalibrateScores() {
        // 1. Reset all players to 0
        for (Player p : this.players) {
            p.updateScore(-p.getScore());
        }

        // 2. Re-apply deltas from all rounds currently in the list
        for (Round round : this.rounds) {
            List<Integer> deltas = round.getScoreDeltas();
            List<Player> roundPlayers = round.getPlayers();

            // Map the deltas back to the players based on their index in the round
            for (int i = 0; i < roundPlayers.size(); i++) {
                Player p = roundPlayers.get(i);
                int delta = deltas.get(i);
                p.updateScore(delta);
            }
        }
    }
    public void notifyTurnPlayed(PlayTurn playTurn) {
        for (GameObserver observer : observers) observer.onTurnPlayed(playTurn);
    }
}