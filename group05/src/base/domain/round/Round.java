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
     * Deals cards and asks for bids, if all players pass start again but the score is doubled but only once
     */
    public void askBids() {
        List<Card> deck = new ArrayList<>(52);
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                deck.add(new Card(suit, rank));
            }
        }
        Collections.shuffle(deck);

        deal(deck);

        List<Bid> playerBids = new ArrayList<>();
        Bid highesbid = null;
        for (Player p : players) {
            Bid newbid = askBid(p, highesbid);
            if (newbid.getType() != BidType.PASS) {
                highesbid = newbid;
                if (highesbid.getType().getCategory() == BidCategory.ABONDANCE) {
                    this.currentPlayer = highesbid.getPlayer();
                }
            }
            playerBids.add(newbid);
            this.highestBid = highesbid;
        }
        this.bids = playerBids;
        this.trumpSuit = highesbid.getChosenTrump(trumpSuit);

        long passCount = bids.stream().filter(b -> b.getType() == BidType.PASS).count();
        long proposeCount = bids.stream().filter(b -> b.getType() == BidType.PROPOSAL).count();

        //check if one player proposes, if no one accepts ask that player if they want to play alone or pass
        //someone else could out-bid the proposal that's why pass count has to be 3
        if (passCount == 3 && proposeCount == 1) {
            Bid proposeBid = bids.stream().filter(b -> b.getType() == BidType.PROPOSAL).findFirst().orElseThrow();
            Bid newBid = proposeBid.getPlayer().chooseBid();
            if (newBid.getType() != BidType.PASS && newBid.getType() != BidType.SOLO_PROPOSAL) {
                throw new IllegalArgumentException();
            }
            for (int i = 0; i < bids.size(); i++) {
                if (bids.get(i).getPlayer().equals(proposeBid.getPlayer())) {
                    bids.set(i, newBid);
                    break;
                }
            }
        }
        //check again if all players passed and reset the round accordingly
        long passCount2 = bids.stream().filter(b -> b.getType() == BidType.PASS).count();
        if (passCount2 == players.size()) {
            for (Player p : players) {
                p.flushHand(); //reset every player's hand
            }
            this.dealer = players.get(players.indexOf(dealer) +1 % 4);
            this.currentPlayer = players.get(players.indexOf(currentPlayer) +1 % 4);
            this.bids = new ArrayList<>(); // reset the bids
            this.multiplier = 2; // set multiplier to 2
            askBids(); //ask again for the bids
        }
    }

    /**
     * @param deck to deal out from
     * @throws IllegalArgumentException when a deck is not 52 cards
     */
    public void deal(List<Card> deck) {
        if (deck.size() != 52) {
            throw new IllegalArgumentException();
        }
        int[] dealPattern = {4, 4, 5};
        Iterator<Card> it = deck.iterator();
        int index = (players.indexOf(dealer) + 1) % 4;
        Card lastDealt = null;
        for (int amountToDeal : dealPattern) {
            // Deal to each of the 4 players
            for (int i = 0 ; i < 4 ; i++) {
                Player p = players.get((index + i) % 4);
                for (int j = 0; j < amountToDeal; j++) {
                    lastDealt = it.next();
                    p.addCard(lastDealt);
                }
            }
        }
        trumpSuit = lastDealt.suit();
    }

    /**
     * Asks a given player which Bid he chooses
     *
     * @param p player
     * @param highestBid current highest bid
     * @return Bid
     */
    public Bid askBid(Player p, Bid highestBid) {
        Bid bid = p.chooseBid();
        if (bid.getType() == BidType.PASS) {
            return bid;
        }
        int comparison = bid.compareTo(highestBid);
        if (comparison < 0) {
            throw new IllegalArgumentException("Can't bid lower than highest bid");
        } else if (comparison > 0) {
            return bid;
        } else {
            if (bid.getType() == BidType.PASS ||
                    bid.getType() == BidType.MISERIE ||
                    bid.getType() == BidType.OPEN_MISERIE) {
                return bid;
            }
        }
        return bid;
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
