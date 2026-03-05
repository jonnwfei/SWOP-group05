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
import base.domain.bid.Bid;
/**
 * @author Seppe De Houwer
 * @since 24/02/26
 */
public class Round {
    private final List<Player> players;
    private Player currentPlayer;
    private Player dealer;

    private List<Trick> playedTricks = new ArrayList<>();
    private Trick currentTrick;

    private List<Bid> bids;
    private Suit trumpSuit;
    private int multiplier;
    private boolean finished;

    /**
     * @param players       a list of the players
     * @param dealer        the player who starts dealing the cards
     * @param currentplayer the player who starts with the first turn
     * @param multiplier    how the scores should be calculated
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
        this.currentTrick = null;
        this.bids = new ArrayList<>();
        this.trumpSuit = null;
        this.multiplier = 1;
        this.finished = false;
    }

    /**
     * Deals cards and asks for bids, if all players pass start again but the score is doubled but only once
     *
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
                highesbid = newbid.getType();
                if (highesbid.getType().getCategory() == BidCategory.ABONDANCE) {
                    this.currentPlayer = highesbid.getPlayer();
                }
            }
            playerBids.add(newbid);
        }
        this.bids = playerBids;
        this.trumpSuit = highesbid.getChosenTrump(trumpSuit);

        long passCount = bids.stream().filter(b -> b.getType() == BidType.PASS).count();
        long proposeCount = bids.stream().filter(b -> b.getType() == BidType.PROPOSAL).count();

        //check if one player proposes, if no one accepts ask that player if they wants to play alone or pass
        //someone else could out-bid the proposal thats why passcount ahs to be 3
        if (passCount == 3 && proposeCount == 1) {
            Bid proposeBid = bids.stream().filter(b -> b.getType() == BidType.PROPOSAL).findFirst().orElseThrow();
            Bid newBid = proposeBid.getPlayer().askBid();
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
                p.flushHand(); //reset every players hand
            }
            this.dealer = players.get(players.indexOf(dealer) +1 % 4);
            this.currentPlayer = players.get(players.indexOf(currentPlayer) +1 % 4);
            this.bids = new ArrayList<>(); // reset the bids
            this.multiplier = 2; // set multiplier to 2
            askBids(); //ask again for the bids
        }
        //filter all bids such that calculatescore() only has to calculate the valid bids in bids
        if (highesbid.getType() == BidType.ACCEPTANCE) {
            bids = bids.stream().filter(b -> b.getType() != BidType.PASS);
        } else {
            bids = bids.stream().filter(b -> b.getType() == highesbid.getType());
        }
        return;
    }

    /**
     * @param deck
     * @throws IllegalArgumentException when a deck is not 52 cards
     */
    private void deal(List<Card> deck) {
        if (deck.size() != 52) {
            throw new IllegalArgumentException();
        }
        Iterator<Card> it = deck.iterator();
        int index = (players.indexOf(dealer) + 1) % 4;
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

    private Bid askBid(Player p, Bid highestBid) {
        Bid bid = p.chooseBid();
        if (bid.getType() == BidType.PASS) {
            return bid;
        }
        int comparison = bid.compareTo(highestBid);
        if (comparison < 0) {
            throw new IllegalArgumentException();
        } else if (comparison > 0) {
            return bid;
        } else if (comparison == 0) {
            if (bid.getType() == BidType.PASS ||
                    bid.getType() == BidType.MISERIE ||
                    bid.getType() == BidType.OPEN_MISERIE) {
                return bid;
            } else {
                throw new IllegalArgumentException();
            }
        }
        return bid;
    }

    /**
     * this plays a round, unmless there are already 13 rounds played.
     */
    public void playRound() {
        if (this.finished) {
            return;
        }
        while (playedTricks.size() < 13) {
            currentTrick = new Trick(currentPlayer, trumpSuit);
            currentTrick.playTrick();
            currentPlayer = currentTrick.getWinningPlayer();
            playedTricks.add(currentTrick);
        }
        this.finished = true;
        calculateScores();
    }

    private void calculateScores() {
        if (finished) {
            return;
        }
        if (bids == )
        long tricksWon = 0;
        for (Bid bid : getBids()) {
            if (bid.getType() == BidType.PROPOSAL || bid.getType() == bidType.ACCEPTANCE) {
                tricksWon += getTricksWon(bid.getPlayer());
            } else {

            }
            int points = bid.calculateBasePoints();
            }
        }
    }
    private long getTricksWon(Player player) {
        return (playedTricks.stream().filter(t -> t.getWinningPlayer().equals(player)).count());
    }
    /**
     * getters voor info van de class
     *
     * @return values
     */
    public List<Player> getPlayers() {
        return players;
    }
    public Player getCurrentPlayer() {
        return currentPlayer;
    }
    public List<Bid> getBids() {
        return bids;
    }
    public boolean isFinished() {
        return this.finished;
    }

    public Suit getTrumpSuit() {
        return trumpSuit;
    }
    public void setTrumpSuit(Suit trump) {
        this.trumpSuit = trump;
    }

    public List<Trick> getTricks() {
        return playedTricks;
    }
    public Trick getLastPlayedTrick() {
        if (playedTricks.isEmpty()) {
            return null;
        }
        return playedTricks.getLast();
    }
}
