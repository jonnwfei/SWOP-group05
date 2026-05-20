package base.domain.round;

import base.domain.bid.Bid;
import base.domain.bid.BidCategory;
import base.domain.bid.BidManager;
import base.domain.bid.BidType;
import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.scores.ScoringRegistry;
import base.domain.trick.Trick;
import base.domain.snapshots.RoundSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static base.domain.WhistRules.MAX_TURNS;

/**
 * Represents a single Round in a game of Whist.
 * Acts as the Aggregate Root/Coordinator for the turn order, trick ledger, and contract.
 *
 * @author Seppe De Houwer, Tommy Wu
 * @since 24/02/26
 */
public class Round {

    /** The 4 players participating in this round. */
    private final List<Player> players;
    private Player currentPlayer;
    private final TrickLedger trickLedger;
    private final BidManager bidManager;
    private RoundContract roundContract;
    private Suit trumpSuit;
    private final int multiplier;
    private RoundOutcome outcome;
    private boolean finished;

    public Round(List<Player> players, Player startingPlayer, int multiplier) {
        if (players == null || players.size() != 4) {
            throw new IllegalArgumentException("Players list must contain exactly 4 players.");
        }
        if (startingPlayer == null || !players.contains(startingPlayer)) {
            throw new IllegalArgumentException("Starting Player must not be null and must be in the players list.");
        }
        if (multiplier < 1) {
            throw new IllegalArgumentException("Multiplier must be at least 1.");
        }
        this.players = new ArrayList<>(players);
        this.currentPlayer = startingPlayer;
        this.trickLedger = new TrickLedger();
        this.bidManager = new BidManager(this.players);
        this.roundContract = null;
        this.trumpSuit = null;
        this.multiplier = multiplier;
        this.outcome = null;
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
        if (this.finished) throw new IllegalStateException("Round already finished.");
        if (highestBid == null) throw new IllegalArgumentException("highestBid cannot be null.");
        if (firstPlayer == null) throw new IllegalArgumentException("firstPlayer cannot be null.");

        List<PlayerId> biddingTeam = bidManager.resolveBiddingTeam();
        List<PlayerId> defendingTeam = new ArrayList<>(this.players.stream().map(Player::getId).toList());
        defendingTeam.removeAll(biddingTeam);

        this.roundContract = new RoundContract(highestBid, biddingTeam, defendingTeam, this.multiplier);
        this.trumpSuit = trumpSuit;
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
        if (this.finished) throw new IllegalStateException("Round already finished.");
        if (finalBids == null || finalBids.size() != 4)
            throw new IllegalArgumentException("Must provide 4 bids for pass-check.");

        if (!finalBids.stream().allMatch(bid -> bid.getType() == BidType.PASS)) {
            throw new IllegalArgumentException("All bids must be PASS.");
        }
        this.players.forEach(Player::flushHand);
        this.finished = true;
    }

    /**
     * Advances the {@code currentPlayer} to the next player in turn order.
     */
    public void advanceToNextPlayer() {
        if (this.finished) throw new IllegalStateException("Round already finished.");
        int currentIdx = players.indexOf(currentPlayer);
        this.currentPlayer = players.get((currentIdx + 1) % 4);
    }

    public void finalizeTrick(Trick trick, ScoringRegistry registry) {
        if (this.finished) throw new IllegalStateException("Cannot add trick to finished round");
        if (trick == null || registry == null) throw new IllegalArgumentException("Arguments cannot be null.");

        this.trickLedger.recordTrick(trick);
        this.currentPlayer = getPlayerById(trick.getWinningPlayerId());

        if (this.trickLedger.isFull() || isMiserieEarlyTermination()) {
            int tricksWon = this.trickLedger.getTricksWonByTeam(this.roundContract.getBiddingTeam());
            List<PlayerId> miserieWinners = new ArrayList<>();
            if (this.roundContract.getWinningBid().getType().getCategory() == BidCategory.MISERIE) {
                List<PlayerId> biddingTeam = this.roundContract.getBiddingTeam();
                for (PlayerId bidderId : biddingTeam) {
                    if (!this.trickLedger.hasPlayerWonAnyTrick(bidderId)) {
                        miserieWinners.add(bidderId);
                    }
                }
            }

            RoundOutcomeFacts facts = new RoundOutcomeFacts(tricksWon, miserieWinners);
            this.processOutcome(facts, registry);
        }
    }

    // =========================================================================
    // Scoring
    // =========================================================================

    /**
     * GRASP Pure Fabrication: Decouples outcome calculation from player state mutation.
     * Updates the internal 'outcome' record. Calling code is responsible for mutating
     * live Player objects if desired.
     */
    private void processOutcome(RoundOutcomeFacts facts, ScoringRegistry registry) {
        Map<PlayerId, Integer> deltasMap = this.roundContract.evaluateOutcome(
                facts.tricksWon(), facts.miserieWinners(), registry
        );

        List<Integer> finalDeltas = new ArrayList<>(Collections.nCopies(4, 0));
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            int delta = deltasMap.getOrDefault(p.getId(), 0);
            finalDeltas.set(i, delta);
        }

        this.outcome = new RoundOutcome(facts, finalDeltas);
        this.finished = true;
    }

    /**
     * Retroactively recalculates this round's scores based on updated global rules.
     * Stateless with respect to Player objects (Bug 5 fix).
     */
    public void recalculateRetroactiveScores(ScoringRegistry registry) {
        if (this.outcome == null || !this.finished) return;

        // Re-process with same facts but new registry parameters
        RoundOutcomeFacts facts = this.outcome.facts();

        Map<PlayerId, Integer> deltasMap = this.roundContract.evaluateOutcome(
                facts.tricksWon(), facts.miserieWinners(), registry
        );

        List<Integer> finalDeltas = new ArrayList<>(Collections.nCopies(4, 0));
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            finalDeltas.set(i, deltasMap.getOrDefault(p.getId(), 0));
        }

        this.outcome = new RoundOutcome(facts, finalDeltas);
    }

    // =========================================================================
    // Accessors
    // =========================================================================

    public BidManager getBidManager() { return bidManager; }
    public List<Player> getPlayers() { return List.copyOf(players); }
    public Player getCurrentPlayer() { return currentPlayer; }
    public List<Bid> getBids() { return bidManager.getAllBids(); }
    public int getMultiplier() { return multiplier; }
    public List<Integer> getScoreDeltas() {
        return outcome != null ? List.copyOf(outcome.scoreDeltas()) : List.of(0, 0, 0, 0);
    }
    public List<Trick> getTricks() { return this.trickLedger.getTricks(); }
    public Trick getLastPlayedTrick() { return this.trickLedger.getLastTrick(); }
    public boolean isFinished() { return finished; }
    public Suit getTrumpSuit() { return trumpSuit; }

    /**
     * Information Expert (Bug 1 Fix): Delegates to BidManager, but correctly handles
     * the 'All PASS' terminal state where manager might return null.
     */
    public Bid getHighestBid() {
        return bidManager.getHighestBid();
    }

    /**
     * Maps a {@link PlayerId} to the actual {@link Player} object.
     *
     * @param id the player's id.
     * @return the player.
     * @throws IllegalStateException if no player with the given id is found.
     */
    public Player getPlayerById(PlayerId id) {
        if (id == null) throw new IllegalArgumentException("PlayerId must not be null.");
        return players.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("PlayerId not found!"));
    }

    public List<Player> getWinningPlayers() {
        if (outcome == null) return List.of();
        List<Player> winners = new ArrayList<>();
        List<Integer> deltas = outcome.scoreDeltas();
        for (int i = 0; i < players.size(); i++) {
            if (deltas.get(i) > 0) {
                winners.add(players.get(i));
            }
        }
        return winners;
    }

    public RoundOutcome getOutcome() { return outcome; }

    // =========================================================================
    // Setters used for count-mode or restoration
    // =========================================================================

    public void resolveManualCount(Bid highestBid, List<PlayerId> biddingTeam, int tricksWon, List<PlayerId> miserieWinners, ScoringRegistry registry) {
        if (this.finished) throw new IllegalStateException("Round already finished.");
        if (highestBid == null || biddingTeam == null || registry == null)
            throw new IllegalArgumentException("Arguments cannot be null.");

        List<PlayerId> defendingTeam = new ArrayList<>(this.players.stream().map(Player::getId).toList());
        defendingTeam.removeAll(biddingTeam);

        this.roundContract = new RoundContract(highestBid, biddingTeam, defendingTeam, this.multiplier);
        RoundOutcomeFacts facts = new RoundOutcomeFacts(tricksWon, miserieWinners == null ? List.of() : miserieWinners);
        this.processOutcome(facts, registry);
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

        if (this.finished) throw new IllegalStateException("Round already finished.");
        if (restoredScoreDeltas == null || restoredScoreDeltas.size() != 4)
            throw new IllegalArgumentException("Must provide exactly 4 score deltas.");

        if (highestBid != null && biddingTeam != null) {
            List<PlayerId> defendingTeam = new ArrayList<>(this.players.stream().map(Player::getId).toList());
            defendingTeam.removeAll(biddingTeam);
            this.roundContract = new RoundContract(highestBid, biddingTeam, defendingTeam, this.multiplier);
        }

        this.trumpSuit = trumpSuit;
        RoundOutcomeFacts facts = new RoundOutcomeFacts(tricksWon, miserieWinners == null ? List.of() : miserieWinners);
        this.outcome = new RoundOutcome(facts, List.copyOf(restoredScoreDeltas));
        this.finished = true;
    }

    /**
     * Snapshot (Bug 2 Fix): Now captures the exact player identities of the round
     * so restoration can map them back to the master roster correctly.
     */
    public RoundSnapshot toSnapshot() {
        Bid highestBid = getHighestBid();
        if (highestBid == null) throw new IllegalStateException("Cannot snapshot a round without a highest bid");

        List<Player> roundPlayers = getPlayers();
        List<String> playerIds = roundPlayers.stream().map(p -> p.getId().id().toString()).toList();

        BidType bidType = highestBid.getType();

        PlayerId bidderId = bidManager.getBidderOf(highestBid);
        int bidderIndex = roundPlayers.indexOf(getPlayerById(bidderId));

        List<PlayerId> biddingTeam = bidManager.resolveBiddingTeam();
        List<Integer> participantIndices = biddingTeam.stream()
                .map(id -> roundPlayers.indexOf(getPlayerById(id)))
                .toList();

        int tricksWon = -1;
        List<Integer> miserieWinnerIndices = new ArrayList<>();
        if (outcome != null) {
            tricksWon = outcome.facts().tricksWon();
            miserieWinnerIndices = outcome.facts().miserieWinners().stream()
                    .map(id -> roundPlayers.indexOf(getPlayerById(id)))
                    .toList();
        }

        return new RoundSnapshot(
                playerIds, bidType, bidderIndex, participantIndices, tricksWon,
                miserieWinnerIndices, multiplier, getScoreDeltas(), trumpSuit);
    }

    public RoundContract getRoundContract() {
        return roundContract;
    }

    // =========================================================================
    // Private Internal Math Engine Helpers
    // =========================================================================

    private boolean isMiserieEarlyTermination() {
        if (this.roundContract == null) {
            return false;
        }

        if (this.roundContract.getWinningBid().getType().getCategory() != BidCategory.MISERIE) {
            return false;
        }

        List<PlayerId> miserieBidders = this.roundContract.getBiddingTeam();
        if (miserieBidders.isEmpty()) {
            return false;
        }

        // Check if ALL miserie bidders have won at least one trick (meaning they all failed)
        for (PlayerId bidderId : miserieBidders) {
            if (!this.trickLedger.hasPlayerWonAnyTrick(bidderId)) {
                return false; // At least one player is still successfully dodging tricks
            }
        }

        return true; // All miserie bidders failed; the round can end early
    }
}