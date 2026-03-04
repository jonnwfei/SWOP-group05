/**
 * A class that represents a round played during the game
 * */
import java.util.*;
import base.domain.bid.Bid;
/**
 * @author Seppe De Houwer
 * @since 24/02/26
 */
public class Round {
    private final List<Player> players;
    private Player currentPlayer;
    private Player dealer;

    private list<Trick> playedTricks = new ArrayList<>();
    private Trick currentTrick;

    private List<Bid> bids;
    private Suit trumpSuit;
    private int multiplier;
    private boolean finished;

    /**
     * @param players
     * @param dealer
     * @param currentplayer
     * @param multiplier
     * @throws IllegalArgumentException if players or dealer is null
     * @throws IllegalArgumentException if there are more than 4 players
     */
    public Round(List<Player> players, Player dealer, Player currentplayer, int multiplier=1) {
        if (players == null || players.size() != 4) {
            throw new IllegalArgumentException();
        }
        this.players = new ArrayList<>(players);
        if (dealer == null){
            throw new IllegalArgumentException();
        }
        else {
            this.dealer = dealer;
        }
        this.trumpSuit = Suit NONE;
        this.multiplier = multiplier;
        this.bids = new ArrayList<>();
    }

    /**
     * Deals cards and asks for bids, if all players pass start again but the score is doubled but only once
     *
     */
    public void askBids() {
        List<Card> deck = new ArrayList<>(52);
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }
        Collections.shuffle(deck);

        deal(deck);

        List<Bid> playerBids = new ArrayList<>();
        Bid highesbid = BidRank.PASS;
        for (Player p : players) {
            Bid newbid = askBid(p, highesbid);
            if (newbid.getRank() != BidRank.PASS) {
                highesbid = newbid;
            }
            playerBids.add(newbid);
        }
        this.bids = playerBids;
        this.trumpSuit = highesbid.getChosenTrump(trumpSuit);

        long passCount = bids.stream().filter(b -> b.getRank() == BidRank.PASS).count();
        long proposeCount = bids.stream().filter(b -> b.getRank() == BidRank.PROPOSE).count();

        //check if one player proposes, if no one accepts ask that player if they wants to play alone or pass
        //someone else could out-bid the proposal thats why passcount ahs to be 3
        if (passCount == 3 && proposeCount == 1) {
            Bid proposeBid = bids.stream().filter(b -> b.getRank() == BidRank.PROPOSE).findFirst();
            Bid newBid = proposeBid.getBidder().askBid();
            if (newBid.getRank() != BidRank.PASS && newBid.getRank() != BidRank.PROPOSE_ALONE) {
                throw new IllegalArgumentException();
            }
            for (int i = 0; i < bids.size(); i++) {
                if (bids.get(i).getBidder().equals(proposeBid.getBidder())) {
                    bids.set(i, newBid);
                    break;
                }
            }
        }
        //check again if all players passed
        long passCount = bids.stream().filter(b -> b.getRank() == BidRank.PASS).count();
        if (passCount == players.size()) {
            for (Player p : players) {
                p.flushHand(); //reset every players hand
            }
            this.bids = new ArrayList<>(); // reset the bids
            this.multiplier = 2; // set multiplier to 2
            askBids(); //ask again for the bids
        }
        return ;
    }

    /**
     * @param deck
     * @throws IllegalArgumentException when a deck is not 52 cards
     */
    public void deal(List<Card> deck) {
        if (deck.size() != 52) {
            throw new IllegalArgumentException();
        }
        Iterator<Card> it = deck.iterator();
        int index = 0;
        Card lastDealt = null;
        while (it.hasNext()) {
            Card c = it.next();
            Player p = players.get(index % 4);
            p.addCard(c);
            lastDealt = c;
            index++;
        }
        trumpSuit = lastDealt.getSuit();
    }

    private Bid askBid(Player p, Bid highestbid) {
        Bid bid = p.askBid();
        if (bid.getRank == BidRank.PASS) {
            return bid;
        }
        int comparison = bid.compareTo(highestbid);
        if (comparison < 0) {
            throw new IllegalArgumentException();
        }
        else if (comparison > 0) {
            return bid;
        }
        else if (comparison = 0) {
            if (bid.bidRank() == BidRank.PASS ||
                    bid.bidRank() == BidRank.MISERIE ||
                    bid.bidRank() == BidRank.OPEN_MISERIE) {
                return bid;
            } else {
                throw new IllegalArgumentException();
            }
        }
    }
    /**
     * this plays a round, unmless there are already 13 rounds played.
     */
    public void playRound() {
        if (this.finished) {
            return();
        }
        while (tricks.size() < 13) {
            currentTrick = new Trick(Player currentPlayer, Suit trumpsuit);
            currentTrick.playTrick();
            currentPlayer = currentTrick.getWinningPlayer();
            tricks.add(currentTrick);
        }
        this.finished = true;
        calculateScores();
        return;
    }
}

public void calculateScores() {
    HashMap<Player, Integer> result = new HashMap<>();
    for (Player p : players) {
        result.put(p, 0);
    }
    if (!finished) {
        return result;
    }
    for (Bid bid : bids) {
        int points = bid.calculatePoints(bid, getTricks());
        Player bidder = bid.getBidder();

        result.put(bidder, result.get(bidder) + points);

        for (Player p : players) {
            if (!p.equals(bidder)) {
                result.put(p, result.get(p) - (points / 3));
            }
        }
    }
    return result;
}

/**
 * getters voor info van de class
 * @return values
 */
public List<Player> getPlayers() {return players; }
public Player getCurrentPlayer() { return currentPlayer; }
public List<Bid> getBids() { return bids; }
public boolean isFinished() { return finished; }
public Suit getTrumpSuit() { return trumpSuit; }
public setTrumpSuit(Suit trump) {trumpSuit = trump; }
public List<Trick> getTricks() { return playedTricks; }
public Trick getLastPlayedTrick() {
    if (tricks.isEmpty()) {
        return null;
    }
    return tricks.get(tricks.size() - 1);
}

}
