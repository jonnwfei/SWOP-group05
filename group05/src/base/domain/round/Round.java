package base.domain.round;

import base.domain.bid.Bid;
import base.domain.bid.BidCategory;
import base.domain.bid.BidManager;
import base.domain.bid.BidType;
import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.trick.Trick;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a single Round in a game of Whist.
 * Owns the per-round {@link BidManager} that links Round, Player and Bid;
 * Round delegates all bid-history reasoning (highest bid, bidding team, partner lookup)
 * to the manager and keeps only play-phase concerns (tricks, scoring, score deltas).
 *
 * @author Seppe De Houwer, Tommy Wu
 * @since 24/02/26
 */
public class Round {
    public static final int MAX_TRICKS = 13;

    private final List<Player> players;
    private final List<Player> biddingTeam;
    private Player currentPlayer;
    private final List<Trick> playedTricks;

    /** Per-round bid history & player↔bid mapping. */
    private final BidManager bidManager;

    /** Cached winning bid; populated by play / count / restore paths (see setters). */
    private Bid highestBid;
    private Suit trumpSuit;

    private final int multiplier;
    private final List<Integer> scoreDeltas;

    private int countTricksWon;
    private List<Player> countMiserieWinners;
    private boolean finished;

    public Round(List<Player> players, Player startingPlayer, int multiplier) {
        if (players == null || players.size() != 4)
            throw new IllegalArgumentException("Players list must contain exactly 4 players.");
        if (startingPlayer == null || !players.contains(startingPlayer))
            throw new IllegalArgumentException("Starting Player must not be null and must be in the players list.");

        this.players = new ArrayList<>(players);
        this.biddingTeam = new ArrayList<>();
        this.currentPlayer = startingPlayer;
        this.playedTricks = new ArrayList<>();
        this.bidManager = new BidManager(this.players);
        this.highestBid = null;
        this.trumpSuit = null;
        this.multiplier = multiplier;
        this.scoreDeltas = new ArrayList<>(List.of(0, 0, 0, 0));
        this.countTricksWon = -1;
        this.countMiserieWinners = new ArrayList<>();
        this.finished = false;
    }

    // =========================================================================
    // Phase transitions
    // =========================================================================

    public void startPlayPhase(List<Bid> finalBids, Bid highestBid, Suit trumpSuit, Player firstPlayer) {
        if (finalBids == null || finalBids.size() != this.players.size())
            throw new IllegalArgumentException("Must have exactly 4 final bids.");
        if (highestBid == null) throw new IllegalArgumentException("Cannot start play phase without a winning bid.");
        if (firstPlayer == null) throw new IllegalArgumentException("Cannot start play phase without a first player.");

        // BidManager was already populated by BidState. The finalBids parameter is now a
        // sanity check — we trust the manager as the source of truth.
        if (bidManager.getAllBids().size() != this.players.size())
            throw new IllegalStateException("BidManager state out of sync with provided finalBids.");

        this.highestBid = highestBid;
        this.trumpSuit = trumpSuit;
        this.currentPlayer = firstPlayer;

        resolveTeams();
    }

    /**
     * Calculates the bidding team based on the highest bid by asking the BidManager
     * who participates. Bid no longer knows about Player.
     */
    private void resolveTeams() {
        if (bidManager.getAllBids().size() != this.players.size())
            throw new IllegalStateException("biddings are not finalized, must be called at the end of bidding phase");

        int totalCards = players.stream().mapToInt(p -> p.getHand().size()).sum();
        if (totalCards != 52)
            throw new IllegalStateException("resolveTeams() can only be called before the play phase begins!");

        bidManager.resolveBiddingTeam().stream()
                .map(this::getPlayerById)
                .forEach(biddingTeam::add);
    }

    public void abortWithAllPass(List<Bid> finalBids) {
        if (finalBids == null) throw new IllegalArgumentException("finalBids must not be null.");
        if (finalBids.size() != this.players.size())
            throw new IllegalArgumentException("Must have exactly 4 final bids.");
        if (!finalBids.stream().allMatch(bid -> bid.getType() == BidType.PASS))
            throw new IllegalArgumentException("all bids must be PASS.");

        // The manager already holds all 4 PASS bids (BidState placed them). We only need
        // to record the all-PASS marker on Round itself for the multiplier carry-over.
        this.highestBid = finalBids.getFirst();
        this.players.forEach(Player::flushHand);
        this.finished = true;
    }

    public void advanceToNextPlayer() {
        int currentIdx = players.indexOf(currentPlayer);
        this.currentPlayer = players.get((currentIdx + 1) % 4);
    }

    public void finalizeTrick(Trick trick) {
        if (trick == null) throw new IllegalArgumentException("Trick must not be null.");
        if (trick.getTurns().size() != Trick.MAX_TURNS)
            throw new IllegalArgumentException("Trick is not completed yet");
        if (this.isFinished()) throw new IllegalStateException("Cannot add trick: The round is already finished");

        this.playedTricks.add(trick);
        this.currentPlayer = getPlayerById(trick.getWinningPlayerId());

        if (shouldAutoFinishRound()) {
            this.finished = true;
            calculateAndDistributeScores();
        }
    }

    // =========================================================================
    // Scoring
    // =========================================================================

    public void calculateScoresForCount(Bid calculatedBid, int tricksWon, List<Player> participants,
                                        List<Player> winningMiseriePlayers) {
        if (calculatedBid == null) throw new IllegalArgumentException("Cannot calculate scores without a bid.");
        if (participants == null || participants.isEmpty() || participants.stream().anyMatch(Objects::isNull))
            throw new IllegalArgumentException("Participants list cannot be null, empty, or contain null elements.");
        if (participants.size() > 4)
            throw new IllegalArgumentException("Cannot have more than 4 participating players.");

        this.highestBid = calculatedBid;
        this.biddingTeam.clear();
        this.biddingTeam.addAll(participants);
        this.countTricksWon = tricksWon;
        this.countMiserieWinners = winningMiseriePlayers == null
                ? new ArrayList<>() : new ArrayList<>(winningMiseriePlayers);

        if (calculatedBid.getType().getCategory() == BidCategory.MISERIE) {
            if (winningMiseriePlayers != null && winningMiseriePlayers.stream().anyMatch(Objects::isNull))
                throw new IllegalArgumentException("Winning Miserie players list cannot contain null elements.");
            for (Player p : participants) {
                boolean hasWon = winningMiseriePlayers != null && winningMiseriePlayers.contains(p);
                int basePoints = hasWon ? calculatedBid.calculateBasePoints(0)
                        : calculatedBid.calculateBasePoints(1);
                distributeScores(basePoints, List.of(p));
            }
        } else {
            if (tricksWon < 0 || tricksWon > Round.MAX_TRICKS)
                throw new IllegalArgumentException("Tricks won must be between 0 and " + Round.MAX_TRICKS + ".");
            distributeScores(calculatedBid.calculateBasePoints(tricksWon), participants);
        }
        this.finished = true;
    }

    private void calculateAndDistributeScores() {
        if (!isFinished()) {
            throw new IllegalStateException(
                    "Cannot calculate scores: expected " + MAX_TRICKS + " tricks but got " + playedTricks.size());
        }
        if (highestBid == null) throw new IllegalStateException("Cannot calculate scores: highestBid is null.");

        if (highestBid.getType().getCategory() == BidCategory.MISERIE) {
            for (Player p : getBiddingTeam()) {
                int tricks = getTricksWonBy(List.of(p));
                int basePoints = highestBid.calculateBasePoints(tricks);
                distributeScores(basePoints, List.of(p));
            }
        } else {
            List<Player> attackers = getBiddingTeam();
            int tricksWon = getTricksWonBy(attackers);
            distributeScores(highestBid.calculateBasePoints(tricksWon), attackers);
        }
    }

    public List<Player> getWinningPlayers() {
        if (!isFinished()) return new ArrayList<>();
        if (highestBid == null) throw new IllegalStateException("Cannot calculate scores: highestBid is null.");

        List<Player> bidders = getBiddingTeam();

        if (highestBid.getType().getCategory() == BidCategory.MISERIE) {
            if (playedTricks.isEmpty()) return List.copyOf(countMiserieWinners);
            List<Player> winners = new ArrayList<>();
            for (Player p : bidders) {
                if (getTricksWonBy(List.of(p)) == 0) winners.add(p);
            }
            return winners;
        }

        int tricksWon = countTricksWon >= 0 ? countTricksWon : getTricksWonBy(bidders);
        if (highestBid.calculateBasePoints(tricksWon) > 0) return bidders;
        List<Player> defenders = new ArrayList<>(this.players);
        defenders.removeAll(bidders);
        return defenders;
    }

    private void distributeScores(int basePoints, List<Player> bidders) {
        if (bidders.size() == 1 && (basePoints * multiplier) % 3 != 0)
            throw new IllegalStateException("Base points must be divisible by 3 for a 1vs3 game to maintain zero-sum!");

        for (int i = 0; i < this.players.size(); i++) {
            Player p = this.players.get(i);
            int delta;
            if (bidders.contains(p)) {
                delta = basePoints * multiplier;
            } else if (bidders.size() == 2) {
                delta = -basePoints * multiplier;
            } else {
                delta = (-basePoints * multiplier) / 3;
            }
            p.updateScore(delta);
            scoreDeltas.set(i, scoreDeltas.get(i) + delta);
        }
    }

    public Player getPlayerById(PlayerId id) {
        return players.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("PlayerId not found!"));
    }

    private List<Player> getBiddingTeam() {
        if (this.biddingTeam.isEmpty())
            throw new IllegalStateException("Teams have not been resolved yet!");
        return this.biddingTeam;
    }

    public int getTricksWonBy(List<Player> team) {
        List<PlayerId> teamIds = team.stream().map(Player::getId).toList();
        int count = 0;
        for (Trick t : playedTricks) {
            if (teamIds.contains(t.getWinningPlayerId())) count++;
        }
        return count;
    }

    // =========================================================================
    // Accessors
    // =========================================================================

    /** The per-round BidManager. Used by BidState during bidding and by callers
     *  that need to ask "who placed this bid?" (e.g. PlayState, persistence). */
    public BidManager getBidManager() { return bidManager; }

    public List<Player> getPlayers() { return List.copyOf(players); }
    public Player getCurrentPlayer() { return currentPlayer; }

    /** All bids placed during this round, in placement order. Delegates to {@link BidManager}. */
    public List<Bid> getBids() { return bidManager.getAllBids(); }

    public List<Player> getBiddingTeamPlayers() { return List.copyOf(biddingTeam); }
    public Bid getHighestBid() { return highestBid; }
    public Suit getTrumpSuit() { return trumpSuit; }
    public int getMultiplier() { return multiplier; }
    public List<Integer> getScoreDeltas() { return List.copyOf(scoreDeltas); }
    public int getCountTricksWon() { return countTricksWon; }

    public int getBiddingTeamTricksWon() {
        if (biddingTeam.isEmpty()) return -1;
        return getTricksWonBy(biddingTeam);
    }

    public List<Player> getCountMiserieWinners() { return List.copyOf(countMiserieWinners); }
    public List<Trick> getTricks() { return List.copyOf(playedTricks); }

    public Trick getLastPlayedTrick() {
        return playedTricks.isEmpty() ? null : playedTricks.getLast();
    }

    // =========================================================================
    // Round-finished detection
    // =========================================================================

    public boolean isFinished() {
        if (finished) return true;
        if (isAllPassFinished() || shouldAutoFinishRound()) {
            this.finished = true;
            return true;
        }
        return false;
    }

    private boolean isAllPassFinished() {
        return highestBid != null
                && highestBid.getType() == BidType.PASS
                && bidManager.getAllBids().size() == players.size();
    }

    private boolean shouldAutoFinishRound() {
        if (playedTricks.size() >= MAX_TRICKS) return true;
        if (highestBid == null) return false;
        if (highestBid.getType().getCategory() == BidCategory.MISERIE) return isMiserieEarlyTermination();
        return false;
    }

    /**
     * In Miserie, the round ends as soon as every player who joined the same Miserie
     * contract has already won at least one trick (their contract is irreversibly broken).
     * Asks the BidManager for the participants instead of inspecting Bid.getPlayerId().
     */
    private boolean isMiserieEarlyTermination() {
        if (highestBid == null || highestBid.getType().getCategory() != BidCategory.MISERIE) return false;

        List<PlayerId> miserieBidders = bidManager.findMiserieParticipants(highestBid.getType());
        if (miserieBidders.isEmpty()) return false;

        for (PlayerId bidderId : miserieBidders) {
            boolean wonATrick = playedTricks.stream()
                    .anyMatch(trick -> trick.getWinningPlayerId().equals(bidderId));
            if (!wonATrick) return false;
        }
        return true;
    }

    public void setHighestBid(Bid bid) { this.highestBid = bid; }

    // =========================================================================
    // Snapshot restore
    // =========================================================================

    public void restoreFromSnapshot(Bid highestBid, Suit trumpSuit, List<Player> participants,
                                    int tricksWon, List<Player> miserieWinners,
                                    List<Integer> restoredScoreDeltas) {
        if (highestBid == null) throw new IllegalArgumentException("Cannot restore round without a highest bid.");
        if (participants == null || participants.isEmpty() || participants.stream().anyMatch(Objects::isNull))
            throw new IllegalArgumentException("Participants list cannot be null, empty, or contain null elements.");
        if (participants.stream().anyMatch(p -> !this.players.contains(p)))
            throw new IllegalArgumentException("All participants must belong to this round's players.");
        if (tricksWon < -1 || tricksWon > MAX_TRICKS)
            throw new IllegalArgumentException("Tricks won must be -1 or between 0 and " + MAX_TRICKS + ".");
        if (restoredScoreDeltas == null || restoredScoreDeltas.size() != this.players.size())
            throw new IllegalArgumentException("Score deltas must contain exactly " + this.players.size() + " entries.");
        if (restoredScoreDeltas.stream().anyMatch(Objects::isNull))
            throw new IllegalArgumentException("Score deltas cannot contain null elements.");
        if (miserieWinners != null && miserieWinners.stream().anyMatch(p -> !participants.contains(p)))
            throw new IllegalArgumentException("Miserie winners must be participants.");

        this.highestBid = highestBid;
        this.trumpSuit = trumpSuit;
        this.biddingTeam.clear();
        this.biddingTeam.addAll(participants);
        this.countTricksWon = tricksWon;
        this.countMiserieWinners = miserieWinners == null ? new ArrayList<>() : new ArrayList<>(miserieWinners);
        this.scoreDeltas.clear();
        this.scoreDeltas.addAll(restoredScoreDeltas);
        this.finished = true;

        // NOTE: GamePersistenceService is responsible for repopulating the BidManager via
        // bidManager.placeBid(...) for the snapshot's bidder before this method runs.
    }
}