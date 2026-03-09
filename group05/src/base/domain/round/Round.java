package base.domain.round;

import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.bid.BidCategory;
import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.trick.Trick;

import java.util.*;

/**
 * Round class, containing List of Players, currentPlayer whose turn it is, List of Played Tricks, List of bids, the highestBid
 * this round's trumpSuit and the multiplier in case all Players PASS.
 *
 * @author Seppe De Houwer
 * @since 24/02/26
 */
public class Round {
    public static final int MAX_TRICKS = 13;

    private final List<Player> players;
    private Player currentPlayer;

    private List<Trick> playedTricks = new ArrayList<>();

    private List<Bid> bids;
    private Bid highestBid;
    private Suit trumpSuit;
    private int multiplier;

    /**
     * @param players       a list of the players
     * @param startingPlayer        the player who starts playing first
     * @throws IllegalArgumentException if players or dealer is null
     * @throws IllegalArgumentException if there are more than 4 players
     */
    public Round(List<Player> players, Player startingPlayer, int multiplier) {
        if (players == null || players.size() != 4) {
            throw new IllegalArgumentException("Players list must contain exactly 4 players.");
        }
        if (startingPlayer == null || !players.contains(startingPlayer)) {
            throw new IllegalArgumentException("Dealer must not be null and must be in the players list.");
        }
        this.players = new ArrayList<>(players);
        this.currentPlayer = startingPlayer;
        this.playedTricks = new ArrayList<>();
        this.bids = new ArrayList<>();
        this.highestBid = null;
        this.trumpSuit = null;
        this.multiplier = multiplier;
    }

    /**
     * Advances the currentPlayer to the next in Turn player.
     */
    public void advanceToNextPlayer() {
        int currentIdx = players.indexOf(currentPlayer);
        this.currentPlayer = players.get((currentIdx +1) %4);
    }

    public void registerCompletedTrick(Trick trick) {
        if (trick.getTurns().size() != Trick.MAX_TURNS) {
            throw new IllegalArgumentException("Trick is not completed yet");
        }
        this.playedTricks.add(trick);
        this.currentPlayer = trick.getWinningPlayer();

        if (this.playedTricks.size() == MAX_TRICKS) {
            calculateScores();
        }
    }

    /**
     * @param tricksWon amount of tricks won by the bidding side (ignored for Miserie)
     *
     * @param participants the players that joined the bid (1 for Solo/Abondance, 2 for Proposal/Acceptance)
     * @param winningMiseriePlayers only used for Miserie: the players from the participants list who got 0 tricks
     */
    public void calculateScoresForCount(int tricksWon, List<Player> participants, List<Player> winningMiseriePlayers) {
        if (highestBid == null) {
            System.err.println("highestBid is null in Round.");
            throw new IllegalStateException("Cannot calculate scores without a bid.");
        }
        // --- CASE 1: MISERIE (Normal or Open) ---
        if (highestBid.getType().getCategory() == BidCategory.MISERIE) {
            if (participants == null || participants.isEmpty()) {
                throw new IllegalArgumentException("Miserie requires at least one participating player.");
            }
            for (Player p : participants) {
                boolean hasWon = winningMiseriePlayers != null && winningMiseriePlayers.contains(p);
                // calculateBasePoints(0) returns positive, calculateBasePoints(1) returns negative
                int basePoints = hasWon ? highestBid.calculateBasePoints(0) : highestBid.calculateBasePoints(1);

                distributeScores(basePoints, List.of(p));
            }
        } else {
            // --- CASE 2: ALL OTHER BIDS (Solo & Partner Bids) ---
            // 'participants' is either size 1 or 2. distributeScores() automatically handles the math
            int points = highestBid.calculateBasePoints(tricksWon);
            distributeScores(points, participants);
        }
    }


    /**
     * Calculates the gained or lost scores for each player, updating them each respectively.
     */
    public void calculateScores() {
        if ( playedTricks.size() != MAX_TRICKS) {
            return;
        }
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
     * Determines which players won the round based on the final trick count.
     * * @return A list of the winning players.
     * For normal bids: Returns the bidders if they met their contract, otherwise returns the other players.
     * For Miserie: Returns only the specific Miserie players who successfully took 0 tricks.
     */
    public List<Player> getWinningPlayers() {
        if (!isFinished()) {
            return new ArrayList<>(); // The round isn't over yet!
        }

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
        if (points > 0) {
            return bidders;
        } else {
            List<Player> defenders = new ArrayList<>(this.players);
            defenders.removeAll(bidders);
            return defenders;
        }
    }

    /**
     *handles 1vs3 and 2vs2
     */
    private void distributeScores(int basePoints, List<Player> bidders) {
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

    /**
     * Extracts the players responsible for the highest bid.
     */
    private List<Player> getBiddingTeam() {
        List<Player> attackers = new ArrayList<>();
        if (highestBid.getType() == BidType.ACCEPTANCE) {
            for (Bid b : bids) {
                if (b.getType() == BidType.PROPOSAL || b.getType() == BidType.ACCEPTANCE) {
                    attackers.add(b.getPlayer());
                }
            }
        } else if (highestBid.getType().getCategory() == BidCategory.MISERIE) {
            for (Bid b : bids) {
                if (b.getType().getCategory() == BidCategory.MISERIE) {
                    attackers.add(b.getPlayer());
                }
            }
        } else {attackers.add(highestBid.getPlayer());}
        return attackers;
    }

    /**
     * Counts how many tricks a specific group of players won.
     */
    private int getTricksWonBy(List<Player> team) {
        int count = 0;
        for (Trick t : playedTricks) {
            if (team.contains(t.getWinningPlayer())) {
                count++;
            }
        }
        return count;
    }

    /**
     * getters voor info van de class //TODO: kdenk niet dat dit mag qua documentatie
     *
     * @return values
     */
    public List<Player> getPlayers() {
        return List.copyOf(players);
    }
    public Player getCurrentPlayer() {
        return currentPlayer;
    }
    public List<Bid> getBids() {
        return List.copyOf(bids);
    }
    public void setBids(List<Bid> bids) {
        if (bids == null) {
            throw new IllegalArgumentException("Bids list cannot be null.");
        }
        this.bids = new ArrayList<>(bids);
    }

    public boolean isFinished() {
        return playedTricks.size() == MAX_TRICKS;
    }
    public Bid getHighestBid() {
        return highestBid;
    }
    public void setHighestBid(Bid highestBid) {
        this.highestBid = highestBid;
    }
    public Suit getTrumpSuit() {
        return trumpSuit;
    }
    public void setTrumpSuit(Suit trump) {
        this.trumpSuit = trump;
    }
    public void setCurrentPlayer(Player player) {this.currentPlayer = player;}
    public List<Trick> getTricks() {
        return List.copyOf(playedTricks);
    }
    public Trick getLastPlayedTrick() {
        if (playedTricks.isEmpty()) {
            return null;
        }
        return playedTricks.getLast();
    }
}
