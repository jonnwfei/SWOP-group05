package base.domain;

import base.domain.bid.BidType;
import base.domain.bid.Bid;
import base.domain.bid.BidManager;
import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.commands.GameCommand;
import base.domain.deck.Deck;

import base.domain.observer.GameObserver;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.results.GameResult;
import base.domain.round.Round;
import base.domain.scores.ScoringParameters;
import base.domain.scores.ScoringRegistry;
import base.domain.states.BidState;
import base.domain.states.CountState;
import base.domain.states.State;
import base.domain.states.StateStep;
import base.domain.turn.BidTurn;
import base.domain.turn.PlayTurn;
import base.domain.snapshots.GameSnapshot;
import base.domain.snapshots.PlayerSnapshot;
import base.domain.snapshots.RoundSnapshot;
import base.domain.snapshots.SaveMode;
import base.domain.strategy.Strategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private final ScoringRegistry scoringRegistry;

    public WhistGame() {
        this.state = null;
        this.allPlayers = new ArrayList<>();
        this.rounds = new ArrayList<>();
        this.observers = new ArrayList<>();
        this.dealerPlayer = null;
        this.scoringRegistry = new ScoringRegistry();
    }

    // --- Player Management ---

    public Player getNextPlayer(Player currentPlayer) {
        List<Player> activePlayers = getPlayers();
        if (currentPlayer == null || !activePlayers.contains(currentPlayer)) {
            throw new IllegalArgumentException("Cannot find next player: current player is not at the table.");
        }
        int currentIndex = activePlayers.indexOf(currentPlayer);
        return activePlayers.get((currentIndex + 1) % activePlayers.size());
    }

    public Player getPlayerById(PlayerId id) {
        if (id == null) throw new IllegalArgumentException("PlayerId cannot be null.");
        return allPlayers.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("PlayerId " + id + " not found in the game."));
    }

    public List<Player> getPlayers() {
        if (allPlayers.size() < REQUIRED_PLAYERS) {
            throw new IllegalStateException("Active table is not ready (needs 4 players).");
        }
        return List.copyOf(allPlayers.subList(0, REQUIRED_PLAYERS));
    }

    public List<Player> getAllPlayers() {
        return List.copyOf(this.allPlayers);
    }

    public int getTotalPlayerCount() {
        return allPlayers.size();
    }

    public List<PlayerId> getPlayerIds() {
        return allPlayers.stream().map(Player::getId).toList();
    }

    public Map<PlayerId, String> getPlayerNamesMap() {
        return allPlayers.stream().collect(Collectors.toUnmodifiableMap(Player::getId, Player::getName));
    }

    public void addPlayer(Player player) {
        if (player == null) throw new IllegalArgumentException("Player cannot be null");
        if (allPlayers.contains(player))
            throw new IllegalArgumentException("Player already in Game");
        this.allPlayers.add(player);
        player.getDecisionStrategy().onJoinGame(this::addObserver);
    }

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

    public Player getLastRoundWinner() {
        if (this.rounds.isEmpty())
            return null;
        List<Player> winningPlayers = rounds.getLast().getWinningPlayers();
        if (winningPlayers.isEmpty())
            return null;
        return winningPlayers.getFirst();
    }

    public Suit dealCards() {
        List<Player> activePlayers = getPlayers();
        if (this.deck == null) throw new IllegalStateException("Cannot deal cards: Deck is not set.");
        if (activePlayers.size() != REQUIRED_PLAYERS) throw new IllegalStateException("Cannot deal cards: Table must have exactly 4 players.");

        this.deck.shuffle();
        List<List<Card>> hands = this.deck.deal();

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
        if (!activePlayers.contains(startingPlayer) || startingPlayer == null)
            throw new IllegalArgumentException("Starting player not at the table or is null.");

        int multiplier = 1;
        if (!this.rounds.isEmpty()) {
            Bid lastBid = getCurrentRound().getHighestBid();
            if (lastBid != null && lastBid.getType() == BidType.PASS) {
                multiplier = 2;
            }
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

    public void nextState() {
        this.state = state.nextState();
    }

    public StateStep executeState() {
        return state.executeState();
    }

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

    public void rotateActivePlayers() {
        if (getTotalPlayerCount() <= REQUIRED_PLAYERS) return;

        int dealerIndex = allPlayers.indexOf(dealerPlayer);
        if (dealerPlayer == null || dealerIndex < 0 || dealerIndex >= REQUIRED_PLAYERS) {
            throw new IllegalStateException("Cannot rotate players: dealer must be one of the active players.");
        }

        Player leavingPlayer = allPlayers.remove(dealerIndex);
        allPlayers.add(leavingPlayer);
        this.dealerPlayer = allPlayers.get(dealerIndex);
    }

    public void recalibrateScores() {
        for (Player p : this.allPlayers) {
            p.updateScore(-p.getScore());
        }

        List<PlayerId> currentPlayerIds = getPlayerIds();

        for (Round round : this.rounds) {
            List<Integer> deltas = round.getScoreDeltas();
            List<Player> roundPlayers = round.getPlayers();

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

    public boolean canRemovePlayer() {
        return (this.allPlayers.size() > REQUIRED_PLAYERS);
    }

    public GameResult advance(GameCommand command) {
        while (!isOver()) {
            StateStep step = (command == null)
                    ? executeState()
                    : executeState(command);
            command = null;
            if (step.shouldTransition()) nextState();
            if (step.hasResult()) return step.result();
        }
        return null;
    }
    public void setFirstPlayerAsDealer() {
        this.dealerPlayer = allPlayers.getFirst();
    }

    public void removePlayerAtIndex(int index) {
        removePlayer(allPlayers.get(index));
    }

    public void applyScoringChange(BidType bidType, ScoringParameters scoringParameters) {
        this.scoringRegistry.updateParameters(bidType, scoringParameters);

        for (Round round : this.rounds) {
            round.recalculateRetroactiveScores(this.scoringRegistry);
        }

        recalibrateScores();
    }

    public ScoringRegistry getScoringRegistry() {
        return scoringRegistry;
    }

    // =================================================================================
    // Game Snapshot Extraction & Restoration
    // =================================================================================

    public GameSnapshot toSnapshot() {
        List<Player> allPlayers = this.getAllPlayers();
        if (allPlayers.stream().anyMatch(Objects::isNull)) {
            throw new IllegalStateException("Cannot create snapshot: allPlayers contains null entries");
        }
        List<PlayerSnapshot> snapshots = allPlayers.stream().map(Player::toSnapshot).toList();

        List<Round> rounds = this.getRounds();
        if (rounds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalStateException("Cannot create snapshot: round history contains null entries");
        }
        List<RoundSnapshot> roundSnapshots = rounds.stream().map(Round::toSnapshot).toList();

        Player dealer = this.getDealerPlayer();
        if (dealer == null) throw new IllegalStateException("Cannot create snapshot of a game with a null dealer player");
        int dealerIndex = allPlayers.indexOf(dealer);
        if (dealerIndex < 0) throw new IllegalStateException("Dealer player must be part of the current players list");

        return new GameSnapshot(dealerIndex, snapshots, roundSnapshots);
    }

    public void restoreGame(GameSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "Snapshot must not be null");
        this.resetPlayers();
        this.resetRounds();

        for (PlayerSnapshot playerSnapshot : snapshot.players()) {
            PlayerId restoredId = PlayerId.fromString(playerSnapshot.id());
            Strategy playerStrategy = Strategy.toStrategy(playerSnapshot.strategyType(), restoredId);

            Player player = new Player(playerStrategy, playerSnapshot.name(), restoredId);
            player.updateScore(playerSnapshot.score());
            this.addPlayer(player);
        }

        restoreRoundHistory(snapshot.rounds());

        if (snapshot.mode() == SaveMode.GAME) {
            this.setDeck(new Deck());
        }

        this.setDealerPlayer(allPlayers.get(snapshot.dealerIndex()));
    }

    private void restoreRoundHistory(List<RoundSnapshot> roundSnapshots) {
        if (roundSnapshots == null || roundSnapshots.isEmpty()) {
            return;
        }

        for (RoundSnapshot snapshot : roundSnapshots) {
            // BUG 2 FIX: Identify the EXACT 4 players who played this round, not just current table
            List<Player> historicalPlayers = snapshot.playerIds().stream()
                    .map(idStr -> getPlayerById(PlayerId.fromString(idStr)))
                    .toList();

            Player mainBidder = historicalPlayers.get(snapshot.bidderIndex());

            List<PlayerId> participantIds = snapshot.participantIndices().stream()
                    .map(i -> historicalPlayers.get(i).getId())
                    .toList();

            List<PlayerId> miserieWinnerIds = snapshot.miserieWinnerIndices().stream()
                    .map(i -> historicalPlayers.get(i).getId())
                    .toList();

            Round restoredRound = new Round(historicalPlayers, mainBidder, snapshot.multiplier());
            BidManager roundBidManager = restoredRound.getBidManager();
            Bid highestBid = null;

            if (snapshot.bidType() == BidType.PASS) {
                for (Player p : historicalPlayers) {
                    Bid passBid = roundBidManager.placeBid(p.getId(), BidType.PASS, null);
                    if (p.equals(mainBidder)) {
                        highestBid = passBid;
                    }
                }
            } else {
                highestBid = roundBidManager.reconstructManualHistory(
                        snapshot.bidType(),
                        snapshot.trumpSuit(),
                        participantIds
                );
            }

            restoredRound.restoreFromSnapshot(
                    highestBid,
                    snapshot.trumpSuit(),
                    participantIds,
                    snapshot.tricksWon(),
                    miserieWinnerIds,
                    snapshot.scoreDeltas());

            this.addRound(restoredRound);
        }
    }
}
