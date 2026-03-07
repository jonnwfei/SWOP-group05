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
        if (highestBid.getType() == BidType.MISERIE || highestBid.getType() == BidType.OPEN_MISERIE) {
            if (participants == null || participants.isEmpty()) {
                throw new IllegalArgumentException("Miserie requires at least one participating player.");
            }
            for (Player p : participants) {
                boolean hasWon = winningMiseriePlayers != null && winningMiseriePlayers.contains(p);
                // calculateBasePoints(0) returns positive, calculateBasePoints(1) returns negative
                int basePoints = hasWon ? highestBid.calculateBasePoints(0) : highestBid.calculateBasePoints(1);

                p.updateScore(basePoints * multiplier);

                // The other 3 players at the table pay or receive from this specific miserie player
                for (Player other : this.players) {
                    if (!participants.contains(other)) {
                        other.updateScore((basePoints * multiplier * -1));

                    }
                }
            }
        }
        // --- CASE 2: PARTNER BIDS (Proposal / Acceptance / SoloProposal) ---
        else if (participants.size() == 2) {
            int points = highestBid.calculateBasePoints(tricksWon);

            for (Player p : this.players) {
                if (participants.contains(p)) {
                    p.updateScore(points * multiplier);
                } else {
                    // The 2 opponents pay the 2 winners
                    p.updateScore(points * multiplier * -1);
                }
            }
        }
        // --- CASE 3: SOLO BIDS (Solo, Abondance, SoloProposal played alone) ---
        else {
            int points = highestBid.calculateBasePoints(tricksWon);
            Player bidder = participants.get(0);

            for (Player p : this.players) {
                if (p.equals(bidder)) {
                    // Bidder receives points from 3 others
                    p.updateScore(points * multiplier);
                } else {
                    // The 3 opponents pay 1/3 each
                    p.updateScore((points * multiplier * -1) / 3);
                }
            }
        }
    }


    /**
     * Calculates the gained or lost scores for each player, updating them each respectively.
     */
    public void calculateScores() {
        if ( playedTricks.size() != MAX_TRICKS) {
            return;
        }
        int tricksWon = 0;
        if (highestBid.getType() == BidType.ACCEPTANCE ) {
            Player playerAcceptance = null;
            Player playerProposal = null;
            for (Bid b : bids) {
                if (b.getType() == BidType.PROPOSAL) {
                    playerProposal = b.getPlayer();
                } else if (b.getType() == BidType.ACCEPTANCE) {
                    playerAcceptance = b.getPlayer();
                }
            }
            for (Trick t : playedTricks) {
                if (t.getWinningPlayer().equals(playerAcceptance) || t.getWinningPlayer().equals(playerProposal)) {
                    tricksWon += 1;
                }
            }
            int points = highestBid.calculateBasePoints(tricksWon);
            for (Player p : players) {
                if (p.equals(playerAcceptance) || p.equals(playerProposal)) {
                    p.updateScore(points * multiplier);
                } else {
                    p.updateScore(points * multiplier * -1);
                }
            }
        } else {
            for (Trick t : playedTricks) {
                if (t.getWinningPlayer().equals(highestBid.getPlayer())) {
                    tricksWon += 1;
                }
            }
            int points = highestBid.calculateBasePoints(tricksWon);
            for (Player p : players) {
                if (p.equals(highestBid.getPlayer())) {
                    p.updateScore(points * multiplier);
                } else {
                    p.updateScore(points * multiplier * -1 / 3);
                }
            }
        }
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
