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
        Objects.requireNonNull(players, "player list cannot be null");
        if (players.isEmpty()) {
            throw new IllegalArgumentException("Player list cannot be null or empty when starting a round.");
        }

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
        if (bidTurn == null) {
            throw new IllegalArgumentException("BidTurn cannot be null.");
        }
        this.bidsMemory.add(bidTurn);
    }

    @Override
    public void onBiddingFinalized(BidType winningBid, List<PlayerId> biddingTeam) {
        if (biddingTeam == null) {
            throw new IllegalArgumentException("Bidding team list cannot be null.");
        }
        this.activeBid = winningBid;
        this.biddingTeam.clear();
        this.biddingTeam.addAll(biddingTeam);
    }

    @Override
    public void onTurnPlayed(PlayTurn playTurn) {
        if (playTurn == null || playTurn.playerId() == null || playTurn.playedCard() == null) {
            throw new IllegalArgumentException("PlayTurn and its contents cannot be null.");
        }

        this.unplayedCards.remove(playTurn.playedCard());

        if (this.currentTrick == null) {
            this.currentTrick = new Trick(playTurn.playerId(), this.currentTrump);
        }

        this.currentTrick.addTurn(playTurn.playerId(), playTurn.playedCard());
    }

    @Override
    public void onTrickCompleted(PlayerId winner) {
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

        return currentTrick.getTurns().stream()
                .anyMatch(t -> t.playerId().equals(playerId));
    }

    public Card getCardPlayedBy(PlayerId playerId) {
        if (currentTrick == null || playerId == null) return null;

        return currentTrick.getTurns().stream()
                .filter(t -> t.playerId().equals(playerId))
                .map(PlayTurn::playedCard)
                .findFirst()
                .orElse(null);
    }

    public BidType getActiveBid() {
        return activeBid;
    }

    public Suit getCurrentTrump() {
        return currentTrump;
    }

    public boolean isPlayerOnBiddingTeam(PlayerId playerId) {
        Objects.requireNonNull(playerId, "playerId cannot be null when checking if in bidding team.");
        return biddingTeam.contains(playerId);
    }

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
        if (card == null || card.suit() == null || card.rank() == null) {
            throw new NullPointerException("Card and its properties cannot be null when evaluating unplayed highest.");
        }

        return unplayedCards.stream()
                .filter(c -> c != null && c.suit() != null && c.rank() != null) // Defensive stream filtering
                .noneMatch(c -> c.suit().equals(card.suit()) && c.rank().compareTo(card.rank()) > 0);
    }
}