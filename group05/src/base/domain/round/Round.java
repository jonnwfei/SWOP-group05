package base.domain.round;

import base.domain.bid.Bid;
import base.domain.bid.BidCategory;
import base.domain.bid.BidType;
import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.trick.Trick;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single Round in a game of Whist.
 * Acts as the Active Coordinator: managing turn order, tracking tricks, and mapping
 * IDs (PlayerId) back to physical Player objects.
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
    private final List<Bid> bids;
    private Bid highestBid;
    private Suit trumpSuit;
    private final int multiplier;

    /**
     * Constructs a new Round of Whist
     *
     * @param players        a list of the players
     * @param startingPlayer the player who starts playing first
     * @param multiplier     the score multiplier for this round (e.g. 2 if all PASS last round)
     * @throws IllegalArgumentException if players or dealer is null
     * @throws IllegalArgumentException if there are more than 4 players
     */
    public Round(List<Player> players, Player startingPlayer, int multiplier) {
        if (players == null || players.size() != 4) {
            throw new IllegalArgumentException("Players list must contain exactly 4 players.");
        }
        if (startingPlayer == null || !players.contains(startingPlayer)) {
            throw new IllegalArgumentException("Starting Player must not be null and must be in the players list.");
        }
        this.players = new ArrayList<>(players);
        this.biddingTeam = new ArrayList<>();
        this.currentPlayer = startingPlayer;
        this.playedTricks = new ArrayList<>();
        this.bids = new ArrayList<>();
        this.highestBid = null;
        this.trumpSuit = null;
        this.multiplier = multiplier;
    }

    /**
     * Finalizes the bidding phase and prepares the round for the playing phase.
     * Validates the inputs, locks in the contracts, and resolves the teams.
     *
     * @param finalBids  The complete list of bids made this round.
     * @param highestBid The winning contract.
     * @param trumpSuit  The active trump suit for the play phase.
     * @param firstPlayer The player who gets to lead the first trick.
     */
    public void startPlayPhase(List<Bid> finalBids, Bid highestBid, Suit trumpSuit, Player firstPlayer) {
        if (finalBids == null || finalBids.size() != this.players.size()) {
            throw new IllegalArgumentException("Must have exactly 4 final bids.");
        } else if (highestBid == null) {
            throw new IllegalArgumentException("Cannot start play phase without a winning bid.");
        } else if (firstPlayer == null) {
            throw new IllegalArgumentException("Cannot start play phase without a first player.");
        }

        this.bids.clear();
        this.bids.addAll(finalBids);
        this.highestBid = highestBid;
        this.trumpSuit = trumpSuit;
        this.currentPlayer = firstPlayer;

        // Automatically build the teams now that the state is locked in!
        resolveTeams();
    }

    /**
     * Calculates the bidding team based on the highest bid.
     * MUST be called at the end of the Bidding Phase, before any cards are played!
     */
    private void resolveTeams() {
        if (this.bids.size() != this.players.size()) {
            throw new IllegalStateException("biddings are not finalized, must be called at the end of bidding phase");
        }

        int totalCards = players.stream().mapToInt(p -> p.getHand().size()).sum();
        if (totalCards != 52) {
            throw new IllegalStateException("resolveTeam() can only be called before the play phase begins!");
        }

        // The bid knows how to build its own team.
        this.biddingTeam.addAll(this.highestBid.getTeam(this.bids, this.players).stream().map(this::getPlayerById).toList());
    }

    /**w
     * Aborts the round because all players passed.
     * Records the final bids and sets the highest bid to PASS for the multiplier tracking.
     *
     * @param finalBids The 4 pass bids.
     */
    public void abortWithAllPass(List<Bid> finalBids) {
        if (finalBids == null) {throw new IllegalArgumentException("finalBids must not be null.");}
        if (finalBids.size() != this.players.size()) {throw new IllegalArgumentException("Must have exactly 4 final bids.");}
        if (!finalBids.stream().allMatch(bid -> bid.getType() == BidType.PASS)) {throw new IllegalArgumentException("all bids must be PASS.");}

        this.bids.addAll(finalBids);
        this.highestBid = finalBids.getFirst();
        this.players.forEach(Player::flushHand);
    }

    /**
     * Advances the currentPlayer to the next in Turn player.
     */
    public void advanceToNextPlayer() {
        int currentIdx = players.indexOf(currentPlayer);
        this.currentPlayer = players.get((currentIdx + 1) % 4);
    }

    /**
     * Registers a completed Trick to this Round's history.
     * The winner of the registered trick automatically becomes the current player for the next trick.
     * If this is the final trick of the round, scores are automatically calculated and distributed.
     *
     * @param trick The completed trick to be added.
     * @throws IllegalArgumentException If the trick has not received exactly 4 turns yet.
     * @throws IllegalStateException    If the round has already reached the maximum number of tricks.
     */
    public void registerCompletedTrick(Trick trick) {
        if (trick == null) {throw new IllegalArgumentException("trick must not be null.");}
        if (trick.getTurns().size() != Trick.MAX_TURNS) throw new IllegalArgumentException("Trick is not completed yet");
        if (this.playedTricks.size() >= MAX_TRICKS) throw new IllegalStateException("Round is already finished");

        this.playedTricks.add(trick);

        // Map the winning PlayerId back to the physical Player object
        PlayerId winnerId = trick.getWinningPlayerId();
        this.currentPlayer = getPlayerById(winnerId);

        if (this.playedTricks.size() == MAX_TRICKS) {
            // Delegate scoring calculation to our Pure Fabrication math engine!
            calculateAndDistributeScores();
        }
    }

    /**
     * Calculates and distributes the scores for a simulated round (Score Counting Use Case).
     * This bypasses the need to actually play 13 tricks and relies on user-provided outcomes.
     *
     * @param calculatedBid         The bid determining the scoring rules.
     * @param tricksWon             Amount of tricks won by the bidding side (0-13, ignored for Miserie).
     * @param participants          The players that joined the bid (1 for Solo/Abondance, 2 for Proposal/Acceptance).
     * @param winningMiseriePlayers Only used for Miserie: the players from the participants list who got 0 tricks.
     * @throws IllegalArgumentException If the bid is null, participants list is invalid, or tricks won is out of bounds.
     */
    //TODO: refactor function, too many parameters
    public void calculateScoresForCount(Bid calculatedBid, int tricksWon, List<Player> participants, List<Player> winningMiseriePlayers) {
        if (calculatedBid == null) {
            throw new IllegalArgumentException("Cannot calculate scores without a bid.");
        }
        if (participants == null || participants.isEmpty() || participants.contains(null)) {
            throw new IllegalArgumentException("Participants list cannot be null, empty, or contain null elements.");
        }
        if (participants.size() > 4) {
            throw new IllegalArgumentException("Cannot have more than 4 participating players.");
        }

        // --- CASE 1: MISERIE (Normal or Open), NO TRICKS WON ---
        if (calculatedBid.getType().getCategory() == BidCategory.MISERIE) {
            if (winningMiseriePlayers != null && winningMiseriePlayers.contains(null)) {
                throw new IllegalArgumentException("Winning Miserie players list cannot contain null elements.");
            }
            for (Player p : participants) {
                boolean hasWon = winningMiseriePlayers != null && winningMiseriePlayers.contains(p);
                int basePoints = hasWon ? calculatedBid.calculateBasePoints(0)
                        : calculatedBid.calculateBasePoints(1);

                distributeScores(basePoints, List.of(p));
            }
        } else {
            // --- CASE 2: ALL OTHER BIDS (Solo & Partner Bids) ---
            if (tricksWon < 0 || tricksWon > Round.MAX_TRICKS) {
                throw new IllegalArgumentException("Tricks won must be between 0 and " + Round.MAX_TRICKS + ".");
            }

            int points = calculatedBid.calculateBasePoints(tricksWon);
            distributeScores(points, participants);
        }
    }


    /**
     * Calculates the points gained or lost based on the tricks played, and updates each player's score accordingly.
     * Miserie bids are calculated individually against the defending players, while other bids are calculated as a team.
     *
     * @throws IllegalStateException if the round has not yet completed playing all 13 tricks
     */
    private void calculateAndDistributeScores() {
        if (playedTricks.size() != MAX_TRICKS) {throw new IllegalStateException("Cannot calculate scores: expected " + MAX_TRICKS + " tricks but got " + playedTricks.size());}
        if (highestBid == null) throw new IllegalStateException("Cannot calculate scores: highestBid is null.");

        // --- CASE 1: MISERIE ---
        if (highestBid.getType().getCategory() == BidCategory.MISERIE) {
            List<Player> miseriePlayers = getBiddingTeam();
            for (Player p : miseriePlayers) {
                // Check tricks for THIS specific player only!
                int tricks = getTricksWonBy(List.of(p));
                int basePoints = highestBid.calculateBasePoints(tricks);
                distributeScores(basePoints, List.of(p));
            }
        }
        // --- CASE 2: NORMAL BIDS (Solo, Partners) ---
        else {
            List<Player> attackers = getBiddingTeam();
            int tricksWon = getTricksWonBy(attackers);
            int points = highestBid.calculateBasePoints(tricksWon);

            // Distribute as a 1v3 or 2v2 game automatically
            distributeScores(points, attackers);
        }
    }

    /**
     * Determines which players successfully won the round based on the final trick count.
     * For normal bids: Returns the bidding team if they met their contract, otherwise returns the defending team.
     * For Miserie: Returns only the specific Miserie players who successfully took 0 tricks.
     *
     * @return A list of the winning players, or an empty list if the round is not yet finished.
     * @throws IllegalStateException when highestBid is null
     */
    public List<Player> getWinningPlayers() {
        if (!isFinished()) return new ArrayList<>(); // The round isn't over yet!

        if (highestBid == null) throw new IllegalStateException("Cannot calculate scores: highestBid is null.");

        List<Player> bidders = getBiddingTeam();

        // --- CASE 1: MISERIE ---
        if (highestBid.getType().getCategory() == BidCategory.MISERIE) {
            List<Player> successfulMiseriePlayers = new ArrayList<>();
            for (Player p : bidders) {
                // In Miserie, you only win if you took exactly 0 tricks
                if (getTricksWonBy(List.of(p)) == 0) {
                    successfulMiseriePlayers.add(p);
                }
            }
            return successfulMiseriePlayers;
        }

        // --- CASE 2: NORMAL BIDS (Solo, Partners) ---
        int tricksWon = getTricksWonBy(bidders);
        int points = highestBid.calculateBasePoints(tricksWon);

        // If points are positive, the Bidding team made their contract!
        if (points > 0) return bidders;

        List<Player> defenders = new ArrayList<>(this.players);
        defenders.removeAll(bidders);
        return defenders;

    }

    /**
     * Helper method to distribute scores for standard team-based bids (1v3 or 2v2).
     *
     * @param basePoints The calculated points to be awarded or deducted.
     * @param bidders    The list of players who form the attacking team.
     * @throws IllegalStateException if basePoints is not divisible by 3 for a 1vs3 game
     */
    private void distributeScores(int basePoints, List<Player> bidders) {
        if (bidders.size() == 1 && (basePoints * multiplier) % 3 != 0) {
            throw new IllegalStateException("Base points must be divisible by 3 for a 1vs3 game to maintain zero-sum!");
        }

        for (Player p : this.players) {
            if (bidders.contains(p)) {
                p.updateScore(basePoints * multiplier);
            } else {
                // If 2 bidders, opponents pay full basePoints.
                // If 1 bidder, 3 opponents pay 1/3 each.
                if (bidders.size() == 2) {
                    p.updateScore(basePoints * multiplier * -1);
                } else {
                    p.updateScore((basePoints * multiplier * -1) / 3);
                }
            }
        }
    }

    // Currently duplicated in other classes to save time. definitely to be refactored in 3rd iteration
    private Player getPlayerById(PlayerId id) {
        return players.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("PlayerId not found!"));
    }

    /**
     * Extracts the team of players responsible for the highest bid.
     *
     * @return A list containing the bidding team members.
     */
    private List<Player> getBiddingTeam() {
        if (this.biddingTeam.isEmpty()) {
            throw new IllegalStateException("Teams have not been resolved yet!");
        }
        return this.biddingTeam;
    }

    /**
     * Counts how many total tricks were won by a specific list of players.
     *
     * @param team The list of players to evaluate.
     * @return The number of tricks won by the provided team.
     */
    public int getTricksWonBy(List<Player> team) {
        List<PlayerId> teamIds = team.stream().map(Player::getId).toList();
        int count = 0;
        for (Trick t : playedTricks) {
            if (teamIds.contains(t.getWinningPlayerId())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Retrieves a shallow copy of this round's players.
     *
     * @return An unmodifiable list of the 4 players in this round.
     */
    public List<Player> getPlayers() {
        return List.copyOf(players);
    }

    /**
     * Retrieves the player whose turn it currently is to bid or play a card.
     *
     * @return The current active Player.
     */
    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    /**
     * Retrieves a shallow copy of all bids placed during this round.
     *
     * @return An unmodifiable list of bids.
     */
    public List<Bid> getBids() {
        return List.copyOf(bids);
    }

    /**
     * Retrieves the highest bid that won the bidding phase.
     *
     * @return The highest Bid, or null if the bidding phase is not yet complete.
     */
    public Bid getHighestBid() {
        return highestBid;
    }

    /**
     * Retrieves the active trump suit for this round.
     *
     * @return The trump Suit, or null if it has not been determined yet.
     */
    public Suit getTrumpSuit() {
        return trumpSuit;
    }

    /**
     * Retrieves a shallow copy of all tricks played so far in this round.
     *
     * @return An unmodifiable list of played Tricks.
     */
    public List<Trick> getTricks() {
        return List.copyOf(playedTricks);
    }

    /**
     * Retrieves the most recently completed trick.
     *
     * @return The last played Trick, or null if no tricks have been played yet.
     */
    public Trick getLastPlayedTrick() {
        if (playedTricks.isEmpty()) {
            return null;
        }
        return playedTricks.getLast();
    }

    /**
     * Checks whether all 13 tricks have been played, signaling the end of the round.
     *
     * @return true if the round is finished, false otherwise.
     */
    public boolean isFinished() {
        return playedTricks.size() == MAX_TRICKS;
    }

    public void setHighestBid(Bid bid) {
        this.highestBid = bid;
    }
}
