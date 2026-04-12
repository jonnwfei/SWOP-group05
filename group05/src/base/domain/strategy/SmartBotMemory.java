package base.domain.strategy;

import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.deck.Deck;
import base.domain.observer.GameObserver;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.trick.Trick;
import base.domain.turn.BidTurn;
import base.domain.turn.PlayTurn;

import java.util.ArrayList;
import java.util.List;

public class SmartBotMemory implements GameObserver {

    private Suit currentTrump;
    private final List<Card> unplayedCards;
    private final List<BidTurn> bidsMemory;
    private final List<PlayTurn> currentTrickPlayTurns;
    private final List<PlayerId> playersAtTable;

    public SmartBotMemory() {
        this.currentTrump = null;
        this.unplayedCards = new Deck().getCards();
        this.bidsMemory = new ArrayList<>();
        this.currentTrickPlayTurns = new ArrayList<>();
        this.playersAtTable = new ArrayList<>();
    }

    // --- Observer Methods (Updating Memory) ---

    @Override
    public void onRoundStarted(List<PlayerId> players) {
        this.currentTrump = null;
        this.bidsMemory.clear();
        this.unplayedCards.clear();
        this.unplayedCards.addAll(new Deck().getCards());
        this.playersAtTable.clear();
        this.playersAtTable.addAll(players);
        this.currentTrickPlayTurns.clear();
    }

    @Override
    public void onTrumpDetermined(Suit trumpSuit) {
        this.currentTrump = trumpSuit;
    }

    @Override
    public void onBidPlaced(BidTurn bidTurn) {this.bidsMemory.add(bidTurn);}

    @Override
    public void onTurnPlayed(PlayTurn playTurn) {
        this.unplayedCards.remove(playTurn.playedCard());
        this.currentTrickPlayTurns.add(playTurn);

        if (this.currentTrickPlayTurns.size() == Trick.MAX_TURNS) {
            this.currentTrickPlayTurns.clear();
        }
    }

    // --- Getters ---

    public Suit getCurrentTrump() { return currentTrump; }

    /**
     * Returns the highest bid placed so far wrapped in a BidTurn.
     */
    public BidTurn getHighestBid() {
        return bidsMemory.stream()
                .max((b1, b2) -> b1.bidType().compareTo(b2.bidType()))
                .orElse(null);
    }

    public Suit getLeadSuit() { return currentTrickPlayTurns.isEmpty() ? null : currentTrickPlayTurns.getFirst().playedCard().suit(); }

    public PlayTurn getCurrentWinningTurn() {
        Suit leadingSuit = getLeadSuit();
        Card bestCard = null;
        PlayTurn winningPlayTurn = null;

        for (PlayTurn playTurn : currentTrickPlayTurns) {
            Card playedCard = playTurn.playedCard();

            if (bestCard == null) {
                bestCard = playedCard;
                winningPlayTurn = playTurn;
                continue;
            }

            boolean isNewCardTrump = (this.currentTrump != null && playedCard.suit() == this.currentTrump);
            boolean isBestCardTrump = (this.currentTrump != null && bestCard.suit() == this.currentTrump);

            if (isNewCardTrump) {
                // Trump always beats non-trump; highest trump beats lower trump
                if (!isBestCardTrump || playedCard.rank().compareTo(bestCard.rank()) > 0) {
                    bestCard = playedCard;
                    winningPlayTurn = playTurn;
                }
            } else if (!isBestCardTrump) {
                // If no trump is involved, highest rank of the leading suit wins
                if (playedCard.suit() == leadingSuit && playedCard.rank().compareTo(bestCard.rank()) > 0) {
                    bestCard = playedCard;
                    winningPlayTurn = playTurn;
                }
            }
        }
        return winningPlayTurn;
    }

// --- Strategy Helpers (Required by SmartBotStrategy) ---

    public boolean hasActiveProposal() {
        return this.bidsMemory.stream()
                .anyMatch(bid -> bid.bidType() == BidType.PROPOSAL);
    }

    public boolean isLeadPlayer() {
        return this.currentTrickPlayTurns.isEmpty();
    }

    public PlayerId calculateCurrentWinnerId() {
        PlayTurn turn = getCurrentWinningTurn();
        return turn != null ? turn.player() : null;
    }

    public boolean hasPlayerActedInCurrentTrick(PlayerId playerId) {
        return this.currentTrickPlayTurns.stream()
                .anyMatch(turn -> turn.player().equals(playerId));
    }

    public Card getCardPlayedBy(PlayerId playerId) {
        return this.currentTrickPlayTurns.stream()
                .filter(turn -> turn.player().equals(playerId))
                .map(PlayTurn::playedCard)
                .findFirst()
                .orElse(null);
    }

    public boolean isHighestUnplayedCardInSuit(Card card) {
        return unplayedCards.stream().anyMatch(c -> c.suit().equals(card.suit()) && c.rank().compareTo(card.rank()) <= 0);
    }

    /**
     * Determines if the asking player's team is currently winning the trick.
     * Evaluates partnerships (like Proposal/Acceptance) using BidTurn history.
     */
    public boolean isTeamWinning(PlayerId askingPlayerId) {
        PlayTurn winningPlayTurn = getCurrentWinningTurn();

        // If the table is empty, no one is winning!
        if (winningPlayTurn == null) {
            return false;
        }

        PlayerId winnerId = winningPlayTurn.player();

        // Did the asking player play the winning card?
        if (askingPlayerId.equals(winnerId)) {
            return true;
        }

        BidTurn highestBid = getHighestBid();
        if (highestBid == null) return false;

        // Check partnerships for Proposal/Acceptance
        if (highestBid.bidType() == BidType.ACCEPTANCE || highestBid.bidType() == BidType.PROPOSAL) {
            PlayerId proposer = bidsMemory.stream()
                    .filter(b -> b.bidType() == BidType.PROPOSAL)
                    .map(BidTurn::playerId)
                    .findFirst()
                    .orElse(null);

            PlayerId acceptor = bidsMemory.stream()
                    .filter(b -> b.bidType() == BidType.ACCEPTANCE)
                    .map(BidTurn::playerId)
                    .findFirst()
                    .orElse(null);

            boolean amIOnTeam = askingPlayerId.equals(proposer) || askingPlayerId.equals(acceptor);
            boolean isWinnerOnTeam = winnerId.equals(proposer) || winnerId.equals(acceptor);

            return amIOnTeam && isWinnerOnTeam;
        }

        // Default to false for SOLO, MISERIE, ABONDANCE (no known partner via BidTurn)
        return false;
    }
}