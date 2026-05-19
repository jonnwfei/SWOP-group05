package base.domain.strategy;

import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.deck.Deck;
import base.domain.observer.GameObserver;
import base.domain.player.PlayerId;
import base.domain.trick.Trick;
import base.domain.trick.TrickEvaluator;
import base.domain.turn.BidTurn;
import base.domain.turn.PlayTurn;

import java.util.ArrayList;
import java.util.List;

/**
 * Acts as the internal memory for a Smart Bot.
 * It listens to the game's event's via the {@link GameObserver} interface and
 * records the state of the round, bids, and tricks so the Strategy can make informed decisions.
 *
 * @author Tommy Wu
 * @since 01/04/2026
 */
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
        if (this.currentTrickPlayTurns.size() == Trick.MAX_TURNS) {
            this.currentTrickPlayTurns.clear();
        }

        this.unplayedCards.remove(playTurn.playedCard());
        this.currentTrickPlayTurns.add(playTurn);
    }

    // --- Getters ---

    /**
     * @return The currently active Trump suit, or null if playing a No-Trump bid (Miserie).
     */
    public Suit getCurrentTrump() { return currentTrump; }

    /**
     * Finds the highest valid bid placed so far in the bidding phase.
     * @return The highest {@link BidTurn}, or null if no bids have been placed.
     */
    public BidTurn getHighestBid() {
        return bidsMemory.stream()
                .max((b1, b2) -> b1.bidType().compareTo(b2.bidType()))
                .orElse(null);
    }

    /**
     * @return The suit of the first card played in the current trick, or null if the trick is empty.
     */
    public Suit getLeadSuit() { return currentTrickPlayTurns.isEmpty() ? null : currentTrickPlayTurns.getFirst().playedCard().suit(); }

    /**
     * Evaluates the current trick and determines who is winning based on Whist rules.
     *
     * @return The PlayTurn of the playerId currently winning, or null if the trick is empty.
     */
    public PlayTurn getCurrentWinningTurn() {
        if (currentTrickPlayTurns.isEmpty()) return null;

        Suit leadSuit = getLeadSuit();

        TrickEvaluator rules = new TrickEvaluator(leadSuit, this.currentTrump);

        PlayTurn winningTurn = null;
        for (PlayTurn turn : currentTrickPlayTurns) {
            if (winningTurn == null || rules.doesBeat(turn.playedCard(), winningTurn.playedCard())) {
                winningTurn = turn;
            }
        }
        return winningTurn;
    }

// --- Strategy Helpers ---

    /**
     * @return true if the CURRENT HIGHEST bid is a PROPOSAL.
     */
    public boolean hasActiveProposal() {
        BidTurn highest = getHighestBid();
        return highest != null && highest.bidType() == BidType.PROPOSAL;
    }

    /**
     * @return true if the bot is the first to play in the current trick.
     */
    public boolean isLeadPlayer() {
        return this.currentTrickPlayTurns.isEmpty();
    }

    /**
     * @return The ID of the playerId currently winning the trick, or null if the trick is empty.
     */
    public PlayerId calculateCurrentWinnerId() {
        PlayTurn turn = getCurrentWinningTurn();
        return turn != null ? turn.playerId() : null;
    }

    /**
     * Checks if a specific playerId has already played a card in the current trick.
     * @param playerId The ID of the playerId to check.
     * @throws IllegalArgumentException if playerId is null.
     */
    public boolean hasPlayerActedInCurrentTrick(PlayerId playerId) {
        if (playerId == null) throw new IllegalArgumentException("playerId cannot be null");

        return this.currentTrickPlayTurns.stream()
                .anyMatch(turn -> turn.playerId().equals(playerId));
    }

    /**
     * Retrieves the specific card played by a playerId in the current trick.
     * @param playerId The ID of the playerId.
     * @return The card played, or null if they haven't played yet.
     * @throws IllegalArgumentException if playerId is null.
     */
    public Card getCardPlayedBy(PlayerId playerId) {
        if (playerId == null) throw new IllegalArgumentException("playerId cannot be null");
        return this.currentTrickPlayTurns.stream()
                .filter(turn -> turn.playerId().equals(playerId))
                .map(PlayTurn::playedCard)
                .findFirst()
                .orElse(null);
    }

    /**
     * verifies if a given card is guaranteed to be the highest unplayed card of its suit.
     * @param card The card to check.
     * @return true if no unplayed card of the same suit has a strictly higher rank.
     * @throws IllegalArgumentException if card is null.
     */
    public boolean isHighestUnplayedCardInSuit(Card card) {
        if (card == null) throw new IllegalArgumentException("Card cannot be null");

        return unplayedCards.stream()
                .noneMatch(c -> c.suit().equals(card.suit()) && c.rank().compareTo(card.rank()) > 0);
    }

    /**
     * Determines if the asking playerId's team is currently winning the trick.
     * Evaluates partnerships (like Proposal/Acceptance) using BidTurn history.
     *
     * @param askingPlayerId The ID of the playerId asking the question.
     * @return true if the playerId or their recognized partner is winning the trick.
     * @throws IllegalArgumentException if askingPlayerId is null.
     */
    public boolean isTeamWinning(PlayerId askingPlayerId) {
        if (askingPlayerId == null) throw new IllegalArgumentException("askingPlayerId cannot be null");

        if (playersAtTable.isEmpty() || bidsMemory.size() < playersAtTable.size()) {
            throw new IllegalStateException("State violation: Cannot evaluate teams before the bidding phase concludes.");
        }

        PlayTurn winningPlayTurn = getCurrentWinningTurn();

        if (winningPlayTurn == null) {
            return false;
        }

        PlayerId winnerId = winningPlayTurn.playerId();

        if (askingPlayerId.equals(winnerId)) {
            return true;
        }

        BidTurn highestBid = getHighestBid();
        if (highestBid == null) throw  new IllegalStateException("Highest Bid hasn't been set yet");

        BidType highestBidType = highestBid.bidType();

        // Check partnerships for Proposal/Acceptance
        if (highestBidType == BidType.ACCEPTANCE || highestBidType == BidType.PROPOSAL) {
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

        // Default to false for SOLO, MISERIE, ABONDANCE, and TROEL (until Troel partner is explicitly mapped)
        return false;
    }
}