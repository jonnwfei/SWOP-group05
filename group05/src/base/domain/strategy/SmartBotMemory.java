package base.domain.strategy;

import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.deck.Deck;
import base.domain.observer.GameObserver;
import base.domain.player.PlayerId;
import base.domain.trick.Trick;
import base.domain.turn.BidTurn;
import base.domain.turn.PlayTurn;

import java.util.*;

/**
 * A highly cohesive state container.
 * It records events exactly as they are published by the Game Engine.
 * It DOES NOT evaluate game rules, determine winners, or make tactical decisions.
 */
public class SmartBotMemory implements GameObserver {

    private Suit currentTrump;
    private final List<Card> unplayedCards;
    private final List<BidTurn> bidsMemory;
    private Trick currentTrick;

    // Identity & Contract State
    private final List<PlayerId> biddingTeam;
    private BidType activeBid;


    public SmartBotMemory() {
        this.unplayedCards = new Deck().getCards();
        this.bidsMemory = new ArrayList<>();
        this.biddingTeam = new ArrayList<>();
    }

    // --- Observer Methods  ---

    @Override
    public void onRoundStarted(List<PlayerId> players) {
        this.currentTrump = null;
        this.activeBid = null;
        this.bidsMemory.clear();
        this.unplayedCards.clear();
        this.unplayedCards.addAll(new Deck().getCards());
        this.biddingTeam.clear();
        this.currentTrick = null;
    }

    @Override
    public void onTrumpDetermined(Suit trumpSuit) {
        this.currentTrump = trumpSuit;
    }

    @Override
    public void onBidPlaced(BidTurn bidTurn) {
        this.bidsMemory.add(bidTurn);
    }

    @Override
    public void onBiddingFinalized(BidType winningBid, List<PlayerId> biddingTeam) {
        this.activeBid = winningBid;
        this.biddingTeam.clear();
        this.biddingTeam.addAll(biddingTeam);
    }

    @Override
    public void onTurnPlayed(PlayTurn playTurn) {
        this.unplayedCards.remove(playTurn.playedCard());

        if (this.currentTrick == null) {
            this.currentTrick = new Trick(playTurn.playerId(), this.currentTrump);
        }

        this.currentTrick.addTurn(playTurn.playerId(), playTurn.playedCard());
    }

    @Override
    public void onTrickCompleted(PlayerId winner) {
        // Record the win and reset the trick buffer for the next round
        this.currentTrick = null;
    }

    // --- Standardized Generic Queries ---

    public Suit getLeadSuit() {
        return currentTrick == null ? null : currentTrick.getLeadingSuit();
    }

    public boolean isLeadPlayer() {
        return currentTrick == null;
    }

    public PlayerId getCurrentWinnerId() {
        return currentTrick == null ? null : currentTrick.getWinningPlayerId();
    }

    public Card getCurrentWinningCard() {
        return currentTrick == null ? null : currentTrick.getCurrentWinningCard();
    }

    public boolean hasPlayerActedInCurrentTrick(PlayerId playerId) {
        if (currentTrick == null || playerId == null) return false;
        return currentTrick.getTurns().stream().anyMatch(t -> t.playerId().equals(playerId));
    }

    public Card getCardPlayedBy(PlayerId playerId) {
        if (currentTrick == null || playerId == null) return null;
        return currentTrick.getTurns().stream()
                .filter(t -> t.playerId().equals(playerId))
                .map(PlayTurn::playedCard)
                .findFirst()
                .orElse(null);
    }

    public BidType getActiveBid() { return activeBid; }

    public Suit getCurrentTrump() { return currentTrump; }

    public boolean isPlayerOnBiddingTeam(PlayerId playerId) { return biddingTeam.contains(playerId); }

    public BidTurn getHighestBid() {
        return bidsMemory.stream()
                .max(Comparator.comparing(BidTurn::bidType))
                .orElse(null);
    }

    public boolean hasActiveProposal() {
        return bidsMemory.stream()
                .max(Comparator.comparing(BidTurn::bidType))
                .map(highest -> highest.bidType() == BidType.PROPOSAL)
                .orElse(false);
    }

    public boolean isHighestUnplayedCardInSuit(Card card) {
        return unplayedCards.stream()
                .noneMatch(c -> c.suit().equals(card.suit()) && c.rank().compareTo(card.rank()) > 0);
    }
}