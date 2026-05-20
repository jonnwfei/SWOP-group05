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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        if (players == null || players.size() != 4) {
            throw new IllegalArgumentException("Players list must contain exactly 4 players.");
        }
        if (startingPlayer == null || !players.contains(startingPlayer)) {
            throw new IllegalArgumentException("Starting Player must not be null and must be in the players list.");
        }
        this.players = new ArrayList<>(players);
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

    public void startPlayPhase(List<Bid> finalBids, Bid highestBid, Suit trumpSuit, Player firstPlayer) {
        if (finalBids == null || finalBids.size() != this.players.size()) {
            throw new IllegalArgumentException("Must have exactly 4 final bids.");
        }
        if (highestBid == null) {
            throw new IllegalArgumentException("Cannot start play phase without a winning bid.");
        }
        if (firstPlayer == null) {
            throw new IllegalArgumentException("Cannot start play phase without a first player.");
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

    public void abortWithAllPass(List<Bid> finalBids) {
        if (finalBids == null || finalBids.size() != this.players.size()) {
            throw new IllegalArgumentException("Must have exactly 4 final bids.");
        }
        if (!finalBids.stream().allMatch(bid -> bid.getType() == BidType.PASS)) {
            throw new IllegalArgumentException("All bids must be PASS.");
        }
        this.players.forEach(Player::flushHand);
        this.finished = true;
    }

    public void advanceToNextPlayer() {
        int currentIdx = players.indexOf(currentPlayer);
        this.currentPlayer = players.get((currentIdx + 1) % 4);
    }

    public void finalizeTrick(Trick trick, ScoringRegistry registry) {
        if (this.finished) {
            throw new IllegalStateException("Cannot add trick to finished round");
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

    public List<Player> getPlayers() { return List.copyOf(players); }
    public Player getCurrentPlayer() { return currentPlayer; }
    public List<Bid> getBids() { return bidManager.getAllBids(); }
    public int getMultiplier() { return multiplier; }
    public List<Integer> getScoreDeltas() { return List.copyOf(scoreDeltas); }
    public List<Trick> getTricks() { return this.trickLedger.getTricks(); }
    public Trick getLastPlayedTrick() { return this.trickLedger.getLastTrick(); }
    public boolean isFinished() { return finished; }

    public Player getPlayerById(PlayerId id) {
        return players.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("PlayerId not found!"));
    }

    /**
     * Determines which players won the round based on the final trick count
     * and the rules of the contract.
     *
     * @param registry The scoring registry containing the latest mathematical rules.
     * @return a list of winning players, or an empty list if the round is not yet finished.
     */
    public List<Player> getWinningPlayers(ScoringRegistry registry) {
        if (!isFinished() || this.roundContract == null) {
            return new ArrayList<>();
        }

        List<PlayerId> winningIds = new ArrayList<>();
        BidType bidType = roundContract.winningBid().getType();

        // 1. Special Case: Miserie
        if (bidType.getCategory() == BidCategory.MISERIE) {
            // In manual count mode without a ledger, use the explicit winners list
            if (this.trickLedger.getTricks().isEmpty() && !this.miserieWinners.isEmpty()) {
                return this.miserieWinners.stream().map(this::getPlayerById).toList();
            }

            // Otherwise, ask the ledger who succeeded (won 0 tricks)
            for (PlayerId bidder : roundContract.biddingTeam()) {
                if (!trickLedger.hasPlayerWonAnyTrick(bidder)) {
                    winningIds.add(bidder);
                }
            }
            return winningIds.stream().map(this::getPlayerById).toList();
        }

        // 2. Standard Contracts
        int tricksWon;
        if (this.countTricksWon >= 0) {
            // Manual count mode
            tricksWon = this.countTricksWon;
        } else {
            // Normal play mode
            tricksWon = trickLedger.getTricksWonByTeam(roundContract.biddingTeam());
        }

        // 3. Ask the scoring registry if those tricks are enough to win points
        int points = registry.getParameters(bidType).calculatePoints(tricksWon);

        // If points are positive, the attacking team succeeded
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
        if (highestBid == null || biddingTeam == null || miserieWinners == null) {
            throw new IllegalArgumentException("Cannot resolve manual count with incomplete data");
        }

        List<PlayerId> defendingTeam = new ArrayList<>(this.players.stream().map(Player::getId).toList());
        defendingTeam.removeAll(biddingTeam);

        this.roundContract = new RoundContract(highestBid, trumpSuit, biddingTeam, defendingTeam, this.multiplier);
        this.countTricksWon = tricksWon;
        this.miserieWinners = new ArrayList<>(miserieWinners);

        this.finished = true;

        // Note: For a fully strict implementation, manual count should also populate a mock TrickLedger
        // or RoundContract needs an overloaded evaluateOutcome that accepts integers directly.
        this.applyFinalScores(registry);
    }

    public void restoreFromSnapshot(
            Bid highestBid,
            Suit  trumpSuit,
            List<PlayerId> biddingTeam,
            int tricksWon,
            List<PlayerId> miserieWinners,
            List<Integer> restoredScoreDeltas) {

        List<PlayerId> defendingTeam = new ArrayList<>(this.players.stream().map(Player::getId).toList());
        defendingTeam.removeAll(biddingTeam);

        this.roundContract = new RoundContract(highestBid, trumpSuit, biddingTeam, defendingTeam, this.multiplier);
        this.countTricksWon = tricksWon;
        this.miserieWinners = miserieWinners == null ? new ArrayList<>() : new ArrayList<>(miserieWinners);

        for (int i = 0; i < this.players.size(); i++) {
            int delta = restoredScoreDeltas.get(i);
            this.players.get(i).updateScore(delta);
            this.scoreDeltas.set(i, delta);
        }

        this.finished = true;
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private boolean isMiserieEarlyTermination() {
        if (this.roundContract == null) {
            return false;
        }

        Bid highestBid = this.bidManager.getHighestBid();
        if (highestBid == null || highestBid.getType().getCategory() != BidCategory.MISERIE) {
            return false;
        }

        List<PlayerId> miserieBidders = this.bidManager.findMiserieParticipants(highestBid.getType());
        if (miserieBidders.isEmpty()) {
            return false;
        }

        for (PlayerId bidderId : miserieBidders) {
            boolean wonATrick = this.trickLedger.hasPlayerWonAnyTrick(bidderId);
            if (!wonATrick) {
                return false;
            }
        }
        return true;
    }
}