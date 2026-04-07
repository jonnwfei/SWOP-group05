package base.domain.table;

import base.domain.bid.Bid;
import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.observer.TableObserver;
import base.domain.player.Player;
import base.domain.trick.Trick;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure Fabrication: Represents the physical table where cards and bids are placed.
 * Acts as the single source of truth for the game state and handles Observer notifications.
 */
public class Table {
    private final List<TableObserver> observers = new ArrayList<>();

    private final List<Bid> bids = new ArrayList<>();
    private Bid highestBid;
    private Suit trumpSuit;

    private final List<Trick> playedTricks = new ArrayList<>();
    private Trick currentTrick;

    public void addObserver(TableObserver observer) {
        this.observers.add(observer);
        // Fire initial setup event
        observer.onRoundStarted();
    }

    // --- Physical Table Actions ---

    public void placeBid(Bid bid) {
    }

    public void setHighestBidAndTrump(Bid highestBid, Suit trumpSuit) {
    }

    public void playCard(Player player, Card card) {
    }

    public void startNewTrick(Player startingPlayer) {
    }

    // --- Getters ---

    public List<Bid> getBids() {
        return bids;
    }

    public Bid getHighestBid() {
        return highestBid;
    }

    public Suit getTrumpSuit() {
        return trumpSuit;
    }

    public List<Trick> getPlayedTricks() {
        return playedTricks;
    }

    public Trick getCurrentTrick() {
        return currentTrick;
    }
}