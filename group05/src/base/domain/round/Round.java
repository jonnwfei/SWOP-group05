package base.domain.round;

import base.domain.bid.Bid;
import base.domain.bid.BidCategory;
import base.domain.bid.BidManager;
import base.domain.bid.BidType;
import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.snapshots.RoundSnapshot;
import base.domain.scores.ScoringRegistry;
import base.domain.trick.Trick;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a single Round in a game of Whist.
 * Acts as the Aggregate Root/Coordinator for the turn order, trick ledger, and contract.
 *
 * @author Seppe De Houwer, Tommy Wu
 * @since 24/02/26
 */
public class Round {

    private final List<Player> players;
    private Player currentPlayer;
    private final TrickLedger trickLedger;
    private final BidManager bidManager;
    private RoundContract roundContract;
    private final int multiplier;
    private final List<Integer> scoreDeltas;

    // Manual count-mode state (used when trick ledger is bypassed)
    private int countTricksWon;
    private List<PlayerId> miserieWinners;
    private boolean finished;

    public Round(List<Player> players, Player startingPlayer, int multiplier) {
        if (players == null || players.size() != 4 || players.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Players list must contain exactly 4 non-null players.");
        }
        if (startingPlayer == null || !players.contains(startingPlayer)) {
            throw new IllegalArgumentException("Starting Player must not be null and must be in the players list.");
        }
        if (multiplier < 1) {
            throw new IllegalArgumentException("Multiplier must be at least 1.");
        }

        // Defensive copy to ensure structural immutability of the player layout
        this.players = List.copyOf(players);
        this.currentPlayer = startingPlayer;
        this.trickLedger = new TrickLedger();
        this.bidManager = new BidManager(this.players);
        this.roundContract = null;
        this.multiplier = multiplier;
        this.scoreDeltas = new ArrayList<>(List.of(0, 0, 0, 0));
        this.countTricksWon = -1;
        this.miserieWinners = new ArrayList<>();
        this.finished = false;
    }

    // =========================================================================
    // Phase transitions
    // =========================================================================

    /**
     * Finalises the bidding phase and prepares the round for the playing phase.
     * The {@link BidManager} was already populated by {@code BidState}; the
     * {@code finalBids} parameter is kept as a sanity check — we trust the manager
     * as the source of truth.
     *
     * @param finalBids   the complete list of bids made this round.
     * @param highestBid  the winning contract.
     * @param trumpSuit   the active trump suit for the play phase.
     * @param firstPlayer the player who gets to lead the first trick.
     * @throws IllegalArgumentException if any argument is invalid.
     * @throws IllegalStateException    if the BidManager is out of sync with {@code finalBids}.
     */
    public void startPlayPhase(List<Bid> finalBids, Bid highestBid, Suit trumpSuit, Player firstPlayer) {
        if (this.finished) throw new IllegalStateException("Cannot start play phase: Round is already finished.");
        Objects.requireNonNull(highestBid, "Winning bid cannot be null.");
        Objects.requireNonNull(firstPlayer, "First player cannot be null.");

        if (finalBids == null || finalBids.size() != this.players.size()) {
            throw new IllegalArgumentException("Must have exactly 4 final bids.");
        }
        if (!this.players.contains(firstPlayer)) {
            throw new IllegalArgumentException("First player must be a participant of this round.");
        }
        if (bidManager.getAllBids().size() != this.players.size()) {
            throw new IllegalStateException("BidManager state out of sync with provided finalBids.");
        }

        List<PlayerId> biddingTeam = bidManager.resolveBiddingTeam();
        List<PlayerId> defendingTeam = new ArrayList<>(this.players.stream().map(Player::getId).toList());
        defendingTeam.removeAll(biddingTeam);

        this.roundContract = new RoundContract(highestBid, trumpSuit, biddingTeam, defendingTeam, this.multiplier);
        this.currentPlayer = firstPlayer;
    }

    /**
     * Aborts the round because all players passed.
     * The {@link BidManager} already holds the 4 PASS bids (placed by BidState);
     * we only need to record the all-PASS marker on Round itself for the
     * multiplier carry-over.
     *
     * @param finalBids the 4 pass bids.
     * @throws IllegalArgumentException if finalBids is null, not exactly 4, or not all PASS.
     */
    public void abortWithAllPass(List<Bid> finalBids) {
        if (this.finished) throw new IllegalStateException("Cannot abort: Round is already finished.");
        if (finalBids == null || finalBids.size() != this.players.size()) {
            throw new IllegalArgumentException("Must have exactly 4 final bids.");
        }
        if (!finalBids.stream().allMatch(bid -> bid != null && bid.getType() == BidType.PASS)) {
            throw new IllegalArgumentException("All bids must be PASS.");
        }

        this.players.forEach(Player::flushHand);
        this.finished = true;
    }

    public void advanceToNextPlayer() {
        if (this.finished) throw new IllegalStateException("Cannot advance turn: Round is already finished.");

        int currentIdx = players.indexOf(currentPlayer);
        this.currentPlayer = players.get((currentIdx + 1) % 4);
    }

    /**
     * Registers a completed Trick to this Round's history.
     * The winner of the trick becomes the next current player.
     * If the round should finish automatically (13 tricks played or Miserie early
     * termination), scores are calculated and the round is marked finished.
     *
     * @param trick the completed trick to be added.
     * @throws IllegalArgumentException if the trick is null or incomplete.
     * @throws IllegalStateException    if the round is already finished.
     */
    public void finalizeTrick(Trick trick) {
        if (trick == null) {
            throw new IllegalArgumentException("Trick must not be null.");
        }
        if (trick.getTurns().size() != Trick.MAX_TURNS) {
            throw new IllegalArgumentException("Trick is not completed yet.");
        }
        if (this.isFinished()) {
            throw new IllegalStateException("Cannot add trick: The round is already finished.");
        }

        this.trickLedger.recordTrick(trick);
        this.currentPlayer = getPlayerById(trick.getWinningPlayerId());

        if (this.trickLedger.isFull() || isMiserieEarlyTermination()) {
            this.finished = true;
            this.applyFinalScores(registry);
        }
    }

    // =========================================================================
    // Scoring
    // =========================================================================

    /**
     * Calculates scores using the RoundContract and applies the deltas to the players.
     */
    public void applyFinalScores(ScoringRegistry registry) {
        Objects.requireNonNull(registry, "ScoringRegistry cannot be null.");
        if (this.roundContract == null || !this.finished) return;

        Map<PlayerId, Integer> newScores = this.roundContract.evaluateOutcome(this.trickLedger, registry);

        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            int freshDelta = newScores.getOrDefault(p.getId(), 0);

            p.updateScore(freshDelta);
            scoreDeltas.set(i, freshDelta);
        }
    }

    /**
     * Retroactively recalculates this round's scores based on updated global rules.
     */
    public void recalculateRetroactiveScores(ScoringRegistry registry) {
        Objects.requireNonNull(registry, "ScoringRegistry cannot be null.");
        if (this.roundContract == null || !this.finished) return;

        // Reset the internal deltas array to 0s and revert player scores
        for (int i = 0; i < scoreDeltas.size(); i++) {
            int oldDelta = scoreDeltas.get(i);
            players.get(i).updateScore(-oldDelta);
            scoreDeltas.set(i, 0);
        }

        // Delegate to standard calculation
        this.applyFinalScores(registry);
    }

    // =========================================================================
    // Accessors
    // =========================================================================

    public BidManager getBidManager() { return bidManager; }
    public RoundContract getRoundContract() { return roundContract; }

    public List<Player> getPlayers() { return players; }
    public Player getCurrentPlayer() { return currentPlayer; }
    public List<Bid> getBids() { return bidManager.getAllBids(); }
    public int getMultiplier() { return multiplier; }
    public List<Integer> getScoreDeltas() { return List.copyOf(scoreDeltas); }
    public List<Trick> getTricks() { return this.trickLedger.getTricks(); }
    public Trick getLastPlayedTrick() { return this.trickLedger.getLastTrick(); }
    public boolean isFinished() { return finished; }

    public Player getPlayerById(PlayerId id) {
        Objects.requireNonNull(id, "PlayerId cannot be null.");
        return players.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("PlayerId not found in round!"));
    }

    /**
     * Determines which players won the round based on the final trick count
     * and the rules of the contract.
     *
     * @param registry The scoring registry containing the latest mathematical rules.
     * @return a list of winning players, or an empty list if the round is not yet finished.
     */
    public List<Player> getWinningPlayers(ScoringRegistry registry) {
        Objects.requireNonNull(registry, "ScoringRegistry cannot be null.");
        if (!isFinished() || this.roundContract == null) {
            return new ArrayList<>();
        }

        List<PlayerId> winningIds = new ArrayList<>();
        BidType bidType = roundContract.winningBid().getType();

        // 1. Special Case: Miserie
        if (bidType.getCategory() == BidCategory.MISERIE) {
            if (this.trickLedger.getTricks().isEmpty() && !this.miserieWinners.isEmpty()) {
                return this.miserieWinners.stream().map(this::getPlayerById).toList();
            }
            for (PlayerId bidder : roundContract.biddingTeam()) {
                if (!trickLedger.hasPlayerWonAnyTrick(bidder)) {
                    winningIds.add(bidder);
                }
            }
            return winningIds.stream().map(this::getPlayerById).toList();
        }

        // 2. Standard Contracts
        int tricksWon = (this.countTricksWon >= 0)
                ? this.countTricksWon
                : trickLedger.getTricksWonByTeam(roundContract.biddingTeam());

        // 3. Ask the scoring registry if those tricks are enough to win points
        int points = registry.getParameters(bidType).calculatePoints(tricksWon);

        if (points > 0) {
            winningIds.addAll(roundContract.biddingTeam());
        } else {
            winningIds.addAll(roundContract.defendingTeam());
        }

        return winningIds.stream().map(this::getPlayerById).toList();
    }

    // =========================================================================
    // Setters used for count-mode or restoration
    // =========================================================================

    public void resolveManualCount(Bid highestBid, Suit trumpSuit, List<PlayerId> biddingTeam, int tricksWon, List<PlayerId> miserieWinners, ScoringRegistry registry) {
        if (this.finished) throw new IllegalStateException("Cannot resolve manual count: Round is already finished.");
        Objects.requireNonNull(highestBid, "Winning bid cannot be null.");
        Objects.requireNonNull(biddingTeam, "Bidding team cannot be null.");
        Objects.requireNonNull(registry, "ScoringRegistry cannot be null.");

        if (tricksWon < -1 || tricksWon > TrickLedger.MAX_TRICKS) {
            throw new IllegalArgumentException("Invalid tricks won value.");
        }

        List<PlayerId> defendingTeam = new ArrayList<>(this.players.stream().map(Player::getId).toList());
        defendingTeam.removeAll(biddingTeam);

        this.roundContract = new RoundContract(highestBid, trumpSuit, biddingTeam, defendingTeam, this.multiplier);
        this.countTricksWon = tricksWon;
        this.miserieWinners = miserieWinners == null ? new ArrayList<>() : new ArrayList<>(miserieWinners);

        this.finished = true;
        this.applyFinalScores(registry);
    }

    // =========================================================================
    // Snapshot restore
    // =========================================================================


    /**
     * Constructs a snapshot of a round for persistence.
     * This currently captures stable metadata and round count compatibility fields.
     * @return RoundSnapshot of the provided round
     * @throws IllegalArgumentException if the round is null
     * @throws IllegalStateException if the round's internal state is corrupted or missing essential data
     */
    public RoundSnapshot toSnapshot( ) {

        if (highestBid == null) throw new IllegalStateException("Cannot snapshot a round without a highest bid");

        if (players.size() != 4) throw new IllegalStateException("Cannot snapshot round without exactly 4 players");

        BidType bidType = highestBid.getType();
        PlayerId bidderId = bidManager.getHighestBidder();
        Player bidder =  bidderId != null ? getPlayerById(bidderId) : currentPlayer;
        int bidderIndex = players.indexOf(bidder);

        List<Integer> participantIndices = biddingTeam.stream()
                .map(players::indexOf).toList();
        List<Integer> miserieWinnerIndices = miserieWinners.stream().map(players::indexOf).toList(); // TODO: should be

        try {
            return new RoundSnapshot(
                    bidType, bidderIndex, participantIndices, countTricksWon, //TODO: will aslo be changed, no more countTricks won etc
                    miserieWinnerIndices, multiplier, scoreDeltas, trumpSuit);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Round contains invalid data: " + e.getMessage());
        }
    }

    /**
     * Rehydrates a historical round from a persisted snapshot without
     * re-running scoring logic.
     * <p>
     * NOTE: {@code GamePersistenceService} is responsible for repopulating the
     * {@link BidManager} via {@code bidManager.placeBid(...)} for the snapshot's
     * bidder before this method runs.
     *
     * @param highestBid          restored winning bid.
     * @param trumpSuit           restored trump suit (can be null for no-trump).
     * @param participants        restored bidding team members.
     * @param tricksWon           restored count-mode tricks won value.
     * @param miserieWinners      restored count-mode miserie winners.
     * @param restoredScoreDeltas restored per-player score deltas.
     */
    public void restoreFromSnapshot(
            Bid highestBid,
            Suit trumpSuit,
            List<PlayerId> biddingTeam,
            int tricksWon,
            List<PlayerId> miserieWinners,
            List<Integer> restoredScoreDeltas) {

        if (this.finished) throw new IllegalStateException("Cannot restore: Round is already finished.");
        Objects.requireNonNull(highestBid, "Highest bid cannot be null.");
        Objects.requireNonNull(biddingTeam, "Bidding team cannot be null.");
        Objects.requireNonNull(restoredScoreDeltas, "Restored score deltas cannot be null.");

        if (restoredScoreDeltas.size() != this.players.size()) {
            throw new IllegalArgumentException("Must provide exactly 4 score deltas.");
        }
        if (tricksWon < -1 || tricksWon > TrickLedger.MAX_TRICKS) {
            throw new IllegalArgumentException("Invalid tricks won value.");
        }

        List<PlayerId> defendingTeam = new ArrayList<>(this.players.stream().map(Player::getId).toList());
        defendingTeam.removeAll(biddingTeam);

        this.roundContract = new RoundContract(highestBid, trumpSuit, biddingTeam, defendingTeam, this.multiplier);
        this.countTricksWon = tricksWon;
        this.miserieWinners = miserieWinners == null ? new ArrayList<>() : new ArrayList<>(miserieWinners);

        for (int i = 0; i < this.players.size(); i++) {
            this.scoreDeltas.set(i, restoredScoreDeltas.get(i)); // TODO: this is becuz round shouldnt restore the scores itself, but the game should apply the deltas to the players.
        }

        this.finished = true;
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private boolean isMiserieEarlyTermination() {
        if (this.roundContract == null) return false;

        Bid highestBid = this.bidManager.getHighestBid();
        if (highestBid == null || highestBid.getType().getCategory() != BidCategory.MISERIE) {
            return false;
        }

        List<PlayerId> miserieBidders = this.bidManager.findMiserieParticipants(highestBid.getType());
        if (miserieBidders.isEmpty()) return false;

        for (PlayerId bidderId : miserieBidders) {
            boolean wonATrick = this.trickLedger.hasPlayerWonAnyTrick(bidderId);
            if (!wonATrick) {
                return false;
            }
        }
        return true;
    }
}