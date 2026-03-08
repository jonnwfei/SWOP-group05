package base.domain.round;

import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.bid.BidCategory;
import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.card.Rank;
import base.domain.player.Player;
import base.domain.trick.Trick;

import java.util.*;

import static base.domain.bid.BidType.MISERIE;
import static base.domain.bid.BidType.OPEN_MISERIE;

/**
 * @author Seppe De Houwer
 * @since 24/02/26
 */
public class Round {
    public static final int MAX_TRICKS = 13;

    private final List<Player> players;
    private Player currentPlayer;
    private Player dealer;

    private List<Trick> playedTricks = new ArrayList<>();

    private List<Bid> bids;
    private Bid highestBid;
    private Suit trumpSuit;
    private int multiplier;

    /**
     * @param players       a list of the players
     * @param dealer        the player who starts dealing the cards
     * @throws IllegalArgumentException if players or dealer is null
     * @throws IllegalArgumentException if there are more than 4 players
     */
    public Round(List<Player> players, Player dealer) {
        if (players == null || players.size() != 4) {
            throw new IllegalArgumentException();
        }
        this.players = new ArrayList<>(players);
        this.dealer = dealer;
        this.currentPlayer = players.get((players.indexOf(dealer) +1) %4);
        this.playedTricks = new ArrayList<>();
        this.bids = new ArrayList<>();
        this.highestBid = null;
        this.trumpSuit = null;
        this.multiplier = 1;
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
            List<Player> miseriePlayers = getAttackingTeam();
            for (Player p : miseriePlayers) {
                // Check tricks for THIS specific player only!
                int tricks = getTricksWonBy(List.of(p));
                int basePoints = highestBid.calculateBasePoints(tricks);
                distributeScores(basePoints, List.of(p));
            }
        }
        // --- CASE 2: NORMAL BIDS (Solo, Partners) ---
        else {
            List<Player> attackers = getAttackingTeam();
            int tricksWon = getTricksWonBy(attackers);
            int points = highestBid.calculateBasePoints(tricksWon);

            // Distribute as a 1v3 or 2v2 game automatically
            distributeScores(points, attackers);
        }
    }

    /**
     *handles 1vs3 and 2vs2
     */
    private void distributeScores(int basePoints, List<Player> attackers) {
        for (Player p : this.players) {
            if (attackers.contains(p)) {
                p.updateScore(basePoints * multiplier);
            } else {
                // If 2 attackers, opponents pay full basePoints.
                // If 1 attacker, 3 opponents pay 1/3 each.
                if (attackers.size() == 2) {
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
    private List<Player> getAttackingTeam() {
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
