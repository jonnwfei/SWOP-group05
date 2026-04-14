package base.domain.strategy;

import base.domain.bid.Bid;
import base.domain.bid.BidCategory;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.CardMath;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.observer.GameObserver;
import base.domain.player.*;
import base.domain.turn.BidTurn;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static base.domain.card.CardMath.*;

/**
 * Encapsulates the AI decision-making algorithms for a simulated Whist player.
 * <p>
 * This class implements the {@link Strategy} interface and uses a localized
 * {@link SmartBotMemory} observer to independently evaluate game states without
 * direct access to the global game engine, ensuring it cannot cheat.
 *
 * @author Tommy Wu, Stan Kestens
 * @since 01/03/2026
 */
public final class SmartBotStrategy implements Strategy {

    /** Internal record for evaluating the optimal trump suit during the bidding phase. */
    private record TrumpEvaluation(Suit suit, int expectedTricks) {}

    private static final int MIN_TRICKS_FOR_ACCEPTANCE = 3;
    private static final int MIN_TRICKS_FOR_PROPOSAL = 5;
    private static final int MIN_TRICKS_FOR_ABONDANCE = 9;

    /** Represents the three core behavioral states the bot can assume during the play phase. */
    private enum PlayTactic {
        NORMAL,       // Playing standard rules, attempting to win tricks for the team.
        MISERIE,      // Attempting to actively lose every trick (has placed a Miserie bid).
        ANTI_MISERIE  // Attempting to force the Miserie bidder to win a trick.
    }

    // --- Internal Memory & State ---
    private PlayTactic currentPlayTactic;
    private final SmartBotMemory memory;
    private final PlayerId myself;
    private final Random random;

    /**
     * Initializes a new Smart Bot.
     *
     * @param myself The unique identifier of the player this strategy is controlling.
     * @throws IllegalArgumentException if the provided PlayerId is null.
     */
    public SmartBotStrategy(PlayerId myself) {
        if (myself == null) {
            throw new IllegalArgumentException("PlayerId can't be null");
        }
        this.myself = myself;
        this.memory = new SmartBotMemory();
        this.currentPlayTactic = PlayTactic.NORMAL;
        this.random = new Random();
    }

    /**
     * Exposes the bot's internal memory so the Game Engine can register it as an event listener.
     * @return The {@link GameObserver} implementation belonging to this bot.
     */
    public GameObserver getGameObserver() {
        return this.memory;
    }

    // --- Public Strategy Methods ---

    /**
     * Evaluates the bot's hand and the current bidding history to determine the optimal bid.
     *
     * @param playerId The player context (used to instantiate the returned Bid object).
     * @return A legally instantiated {@link Bid}.
     * @throws IllegalArgumentException if the provided player is null.
     */
    @Override
    public Bid determineBid(PlayerId playerId, List<Card> hand) {
        if (playerId == null) {
            throw new IllegalArgumentException("Player cannot be null.");
        }

        BidType miserieBidType = evaluateMiserieEligibility(hand);
        if (miserieBidType != null) {
            return miserieBidType.instantiate(playerId, null);
        }

        int tricksWithCurrentTrump = estimateWinningTricks(hand, memory.getCurrentTrump());
        TrumpEvaluation bestEvaluation = findBestTrumpSuit(hand, tricksWithCurrentTrump);

        if (bestEvaluation.expectedTricks() >= MIN_TRICKS_FOR_ABONDANCE) {
            return mapToHighBid(playerId, bestEvaluation.expectedTricks(), bestEvaluation.suit());
        }

        if (tricksWithCurrentTrump >= MIN_TRICKS_FOR_ACCEPTANCE && memory.hasActiveProposal()) {
            return BidType.ACCEPTANCE.instantiate(playerId, null);
        } else if (tricksWithCurrentTrump >= MIN_TRICKS_FOR_PROPOSAL) {
            return BidType.PROPOSAL.instantiate(playerId, null);
        }

        return BidType.PASS.instantiate(playerId, null);
    }

    /**
     * Determines the optimal card to play for the current trick based on the active tactic.
     *
     * @param currentHand The bot's current hand of cards.
     * @param lead The suit led in the current trick (null if the bot is leading).
     * @return The mathematically optimal {@link Card} to play.
     * @throws IllegalArgumentException if currentHand is null or empty.
     */
    @Override
    public Card chooseCardToPlay(List<Card> currentHand, Suit lead) {
        if (currentHand == null || currentHand.isEmpty()) {
            throw new IllegalArgumentException("Cannot choose a card from an empty or null hand.");
        }

        updateCurrentTactic();
        List<Card> legalCards = CardMath.getLegalCards(currentHand, lead);

        if (legalCards.isEmpty()) {
            throw new IllegalStateException("Critical Error: Legal cards filtered to empty list.");
        }

        // Centralized dispatch to highly cohesive tactical methods
        return switch (currentPlayTactic) {
            case NORMAL        -> playToWinTrick(legalCards, lead);
            case MISERIE       -> playToLoseTrick(legalCards, lead);
            case ANTI_MISERIE  -> playToForceMiserieLoss(legalCards, lead);
        };
    }

    @Override
    public boolean requiresConfirmation() {
        return false;
    }

    // --- State Routing Helper ---

    /**
     * Updates the bot's internal tactical posture based on the outcome of the bidding phase.
     */
    private void updateCurrentTactic() {
        BidTurn highestBidTurn = memory.getHighestBid();

        if (highestBidTurn != null && highestBidTurn.bidType().getCategory() == BidCategory.MISERIE) {
            if (highestBidTurn.playerId().equals(myself)) {
                this.currentPlayTactic = PlayTactic.MISERIE;
            } else {
                this.currentPlayTactic = PlayTactic.ANTI_MISERIE;
            }
        } else {
            this.currentPlayTactic = PlayTactic.NORMAL;
        }
    }

    // --- Tactical Playing Helpers ---

    /**
     * Heuristic for standard Whist play. Attempts to win tricks for the team while conserving high cards.
     */
    private Card playToWinTrick(List<Card> legalCards, Suit lead) {
        if (memory.isLeadPlayer()) {
            Card guaranteedWinner = findGuaranteedWinningLead(legalCards);
            if (guaranteedWinner != null) return guaranteedWinner;
            return getRandomCard(findLowestCards(legalCards));
        }

        if (memory.isTeamWinning(myself)) {
            // Conserve high cards if partner is already winning
            return getRandomCard(findLowestCards(legalCards));
        } else {
            Suit trump = memory.getCurrentTrump();
            boolean isVoidInLead = lead != null && legalCards.stream().noneMatch(c -> c.suit() == lead);

            if (isVoidInLead) {
                Card lowestTrump = findLowestTrump(legalCards, trump);
                if (lowestTrump != null) return lowestTrump;

                // If forced to discard, dump the lowest possible card
                return getRandomCard(findLowestCards(legalCards));
            }

            // Must follow suit; attempt to win by playing the highest card
            return getRandomCard(findHighestCards(legalCards));
        }
    }

    /**
     * Heuristic for Miserie play. The bot actively attempts to dodge winning the trick.
     */
    private Card playToLoseTrick(List<Card> legalCards, Suit lead) {
        if (memory.isLeadPlayer()) {
            return getRandomCard(findLowestCards(legalCards));
        }

        Card currentWinningCard = getWinningCard();
        // Miserie has no trumps, pass null.
        Card highestSafe = findHighestSafeCard(legalCards, currentWinningCard, lead, null);

        // Play the highest card that doesn't win. If forced to win, dump the absolute highest card.
        return highestSafe != null ? highestSafe : getRandomCard(findHighestCards(legalCards));
    }

    /**
     * Heuristic for Anti-Miserie play. The bot colludes to force the Miserie bidder to win.
     */
    private Card playToForceMiserieLoss(List<Card> legalCards, Suit lead) {
        BidTurn highestBid = memory.getHighestBid();
        if (highestBid == null) return getRandomCard(findLowestCards(legalCards)); // Fallback

        PlayerId miseriePlayerId = highestBid.playerId();

        if (!memory.hasPlayerActedInCurrentTrick(miseriePlayerId)) {
            return getRandomCard(findLowestCards(legalCards));
        }

        // We play after the Miserie player
        if (miseriePlayerId.equals(memory.calculateCurrentWinnerId())) {
            Card miserieCard = memory.getCardPlayedBy(miseriePlayerId);

            // Try to play our lowest card that keeps the Miserie player winning
            Card lowestSafeCard = findLowestSafeCard(legalCards, miserieCard, lead, null);
            return lowestSafeCard != null ? lowestSafeCard : getRandomCard(findHighestCards(legalCards));
        } else {
            // Miserie player is currently safe, play the highest legal card to win the trick and lead the next
            return getRandomCard(findHighestCards(legalCards));
        }
    }

    // --- Card Filtering & Math Tools (Pure Fabrication) ---

    /** Selects a random card from a list of tied cards to ensure unpredictable bot behavior. */
    private Card getRandomCard(List<Card> tiedCards) {
        if (tiedCards == null || tiedCards.isEmpty()) {
            throw new IllegalStateException("Cannot select a random card from an empty list.");
        }
        return tiedCards.get(this.random.nextInt(tiedCards.size()));
    }

    private Card findHighestSafeCard(List<Card> options, Card cardToLoseTo, Suit lead, Suit trump) {
        return options.stream()
                .filter(c -> !CardMath.doesCardBeat(c, cardToLoseTo, lead, trump)) // FIXED: using !doesCardBeat to find a losing card
                .max(Comparator.comparing(Card::rank))
                .orElse(null);
    }

    private Card findLowestSafeCard(List<Card> options, Card cardToLoseTo, Suit lead, Suit trump) {
        return options.stream()
                .filter(c -> !CardMath.doesCardBeat(c, cardToLoseTo, lead, trump)) // FIXED: using !doesCardBeat to find a losing card
                .min(Comparator.comparing(Card::rank))
                .orElse(null);
    }

    private Card findLowestTrump(List<Card> cards, Suit trumpSuit) {
        if (trumpSuit == null) return null;
        return cards.stream()
                .filter(c -> c.suit() == trumpSuit)
                .min(Comparator.comparing(Card::rank))
                .orElse(null);
    }

    private Card findGuaranteedWinningLead(List<Card> cards) {
        for (Card card : cards) {
            if (memory.isHighestUnplayedCardInSuit(card)) {
                return card;
            }
        }
        return null;
    }

    private Card getWinningCard() {
        var winningTurn = memory.getCurrentWinningTurn();
        return winningTurn != null ? winningTurn.playedCard() : null;
    }

    // --- Bidding Helpers ---

    private BidType evaluateMiserieEligibility(List<Card> hand) {
        Card highestCard = hand.stream()
                .max(Comparator.comparing(Card::rank))
                .orElseThrow(() -> new IllegalArgumentException("Hand is empty"));

        if (highestCard.rank().compareTo(Rank.SEVEN) <= 0) return BidType.OPEN_MISERIE;
        if (highestCard.rank().compareTo(Rank.TEN) <= 0) return BidType.MISERIE;

        return null;
    }

    private int estimateWinningTricks(List<Card> hand, Suit trumpSuit) {
        int winningTricks = 0;
        for (Card card : hand) {
            if (trumpSuit != null && card.suit() == trumpSuit) {
                winningTricks++;
            } else if (card.rank().compareTo(Rank.JACK) >= 0) {
                winningTricks++;
            }
        }
        return winningTricks;
    }

    private TrumpEvaluation findBestTrumpSuit(List<Card> hand, int tricksWithDealtTrump) {
        Suit bestSuit = memory.getCurrentTrump();
        int maxTricks = tricksWithDealtTrump;

        for (Suit suit : Suit.values()) {
            int tricks = estimateWinningTricks(hand, suit);
            if (tricks > maxTricks) {
                maxTricks = tricks;
                bestSuit = suit;
            }
        }
        return new TrumpEvaluation(bestSuit, maxTricks);
    }

    private Bid mapToHighBid(PlayerId playerId, int tricks, Suit chosenTrump) {
        return switch (tricks) {
            case 9  -> BidType.ABONDANCE_9.instantiate(playerId, chosenTrump);
            case 10 -> BidType.ABONDANCE_10.instantiate(playerId, chosenTrump);
            case 11 -> BidType.ABONDANCE_11.instantiate(playerId, chosenTrump);
            case 12 -> BidType.ABONDANCE_12_OT.instantiate(playerId, chosenTrump);
            case 13 -> {
                if (chosenTrump == memory.getCurrentTrump()) {
                    yield BidType.SOLO_SLIM.instantiate(playerId, chosenTrump);
                } else {
                    yield BidType.SOLO.instantiate(playerId, chosenTrump);
                }
            }
            default -> throw new IllegalArgumentException("Invalid tricks value: " + tricks);
        };
    }
}