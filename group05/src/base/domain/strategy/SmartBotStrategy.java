package base.domain.strategy;

import base.domain.bid.Bid;
import base.domain.bid.BidCategory;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.CardMath;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.observer.GameEventPublisher;
import base.domain.player.PlayerId;
import base.domain.player.TeamRole;
import base.domain.snapshots.StrategySnapshotType;
import base.domain.turn.BidTurn;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Encapsulates the AI decision-making algorithms for a simulated Whist player.
 * <p>
 * This class implements the {@link Strategy} interface and uses a localized
 * {@link SmartBotMemory} data store to independently evaluate game states.
 *
 * @author Tommy Wu, Stan Kestens
 * @since 01/03/2026
 */
public final class SmartBotStrategy implements Strategy {

    /** Internal record for evaluating the optimal trump suit during the bidding phase. */
    private record TrumpEvaluation(Suit suit, int expectedTricks) {}

    enum PlayTactic { NORMAL, MISERIE, ANTI_MISERIE }

    private static final int MIN_TRICKS_FOR_ACCEPTANCE = 3;
    private static final int MIN_TRICKS_FOR_PROPOSAL = 5;
    private static final int MIN_TRICKS_FOR_ABONDANCE = 9;

    // --- Internal State & Memory ---
    private final SmartBotMemory memory;
    private final Random random;

    // Tracks the bot's own bid since it does not know its PlayerId
    /**
     * Initializes a new Smart Bot.
     */
    public SmartBotStrategy() {
        this.memory = new SmartBotMemory();
        this.random = new Random();
    }

    // --- Public Strategy Methods ---

    /**
     * Evaluates the bot's hand and the current bidding history to determine the optimal bid.
     *
     * @return A legally instantiated {@link Bid}.
     * @throws IllegalArgumentException if the provided player is null.
     */
    @Override
    public Bid determineBid(List<Card> hand) {
        Objects.requireNonNull(hand, "Hand cannot be null when determining a bid.");
        if (hand.isEmpty()) throw new IllegalArgumentException("Hand cannot be empty when determining a bid.");

        BidType miserieBidType = evaluateMiserieEligibility(hand);
        if (isLegalBid(miserieBidType)) {
            return miserieBidType.instantiate(null);
        }

        int tricksWithCurrentTrump = estimateWinningTricks(hand, memory.getCurrentTrump());
        TrumpEvaluation bestEvaluation = findBestTrumpSuit(hand, tricksWithCurrentTrump);

        if (bestEvaluation.expectedTricks() >= MIN_TRICKS_FOR_ABONDANCE) {
            Bid highBid = mapToHighBid(bestEvaluation.expectedTricks(), bestEvaluation.suit());
            if (isLegalBid(highBid.getType())) {
                return highBid;
            }
        }

        if (tricksWithCurrentTrump >= MIN_TRICKS_FOR_ACCEPTANCE && memory.hasActiveProposal() && isLegalBid(BidType.ACCEPTANCE)) {
            return BidType.ACCEPTANCE.instantiate(null);
        }

        if (tricksWithCurrentTrump >= MIN_TRICKS_FOR_PROPOSAL && !memory.hasActiveProposal() && isLegalBid(BidType.PROPOSAL)) {
            return BidType.PROPOSAL.instantiate(null);
        }

        return BidType.PASS.instantiate( null);
    }

    /**
     * Determines the optimal card to play for the current trick based on the active tactic.
     *
     * @param currentHand The bot's current hand of cards.
     * @param lead        The suit led in the current trick (null if the bot is leading).
     * @param role
     * @return The mathematically optimal {@link Card} to play.
     * @throws IllegalArgumentException if currentHand is null or empty.
     */
    @Override
    public Card chooseCardToPlay(List<Card> currentHand, Suit lead, TeamRole role) {
        Objects.requireNonNull(currentHand, "Cannot choose a card from a null hand.");
        if (currentHand.isEmpty()) {
            throw new IllegalArgumentException("Cannot choose a card from an empty hand.");
        }

        PlayTactic tactic = determineCurrentTactic(role);
        List<Card> legalCards = CardMath.getLegalCards(currentHand, lead);

        if (legalCards == null || legalCards.isEmpty()) {
            throw new IllegalStateException("Critical Error: Legal cards filtered to empty list.");
        }

        return switch (tactic) {
            case NORMAL        -> playToWinTrick(legalCards, lead, role);
            case MISERIE       -> playToLoseTrick(legalCards, lead);
            case ANTI_MISERIE  -> playToForceMiserieLoss(legalCards, lead);
        };
    }

    /**
     * Returns the snapshot type for this strategy, used for serialization and game state representation.
     * @return the enum value for SMART_BOT
     */
    public StrategySnapshotType toSnapshotType() {
        return StrategySnapshotType.SMART_BOT;
    }

    @Override
    public void onJoinGame(GameEventPublisher publisher) {
        Objects.requireNonNull(publisher, "GameEventPublisher cannot be null when joining a game.");
        publisher.addObserver(memory);
    }

    @Override
    public void onLeaveGame(GameEventPublisher publisher) {
        Objects.requireNonNull(publisher, "GameEventPublisher cannot be null when leaving a game.");
        publisher.removeObserver(memory);
    }

    // --- State & Rule Evaluation (Strategy as the Brain) ---


    private PlayTactic determineCurrentTactic(TeamRole role) {
        BidType activeBidType = memory.getActiveBid();
        if (activeBidType != null && activeBidType.getCategory() == BidCategory.MISERIE) {
            return role == TeamRole.BIDDING_TEAM ? PlayTactic.MISERIE : PlayTactic.ANTI_MISERIE;
        }
        return PlayTactic.NORMAL;
    }


    private boolean isTeamWinningCurrentTrick(TeamRole role) {
        PlayerId winningPlayer = memory.getCurrentWinnerId();
        if (winningPlayer == null) return false;

        boolean amIOnActiveBidType = role == TeamRole.BIDDING_TEAM;
        boolean isWinnerOnActiveBidType = memory.isPlayerOnBiddingTeam(winningPlayer);

        return amIOnActiveBidType == isWinnerOnActiveBidType;
    }

    // --- Tactical Playing Helpers ---

    /**
     * Heuristic for standard Whist play.
     * The objective is to win tricks for the team while conserving high-value cards.
     */
    private Card playToWinTrick(List<Card> legalCards, Suit lead, TeamRole role) {
        if (memory.isLeadPlayer()) {
            Card guaranteedWinner = findGuaranteedWinningLead(legalCards);
            return guaranteedWinner != null ? guaranteedWinner : playLowest(legalCards);
        }

        if (isTeamWinningCurrentTrick(role)) {
            return playLowest(legalCards);
        }

        boolean isVoidInLead = lead != null && CardMath.countCardsOfSuit(legalCards, lead) == 0;

        if (isVoidInLead) {
            Suit trump = memory.getCurrentTrump();
            Card lowestTrump = trump != null ? CardMath.findLowestCardOfSuit(legalCards, trump) : null;

            if (lowestTrump != null) {
                return lowestTrump; // Safely ruff the trick
            }
            return playLowest(legalCards); // Cannot follow suit, cannot trump -> Dump
        }

        // 4. DEFAULT AGGRESSION: We must follow suit (or are off-suit but no trumps exist). Play to win.
        return playHighest(legalCards);
    }

    /**
     * Heuristic for Miserie play.
     * The objective is to actively dodge winning the trick, as the bot has bid Miserie.
     *
     * @param legalCards The filtered list of cards the bot is legally allowed to play.
     * @return The optimal card to play.
     */
    private Card playToLoseTrick(List<Card> legalCards, Suit lead) {
        if (memory.isLeadPlayer()) {
            return getRandomCard(CardMath.findLowestCards(legalCards));
        }

        Card highestLosing = CardMath.findHighestLosingCard(legalCards, memory.getCurrentWinningCard(),lead, null);

        if (highestLosing != null) {
            return highestLosing;
        }

        return getRandomCard(CardMath.findHighestCards(legalCards));
    }

    /**
     * Heuristic for Anti-Miserie play.
     * The objective is to collude with the table to force the Miserie bidder to win the trick.
     *
     * @param legalCards The filtered list of cards the bot is legally allowed to play.
     * @return The optimal card to play.
     */
    private Card playToForceMiserieLoss(List<Card> legalCards, Suit lead) {
        BidTurn highestBid = memory.getHighestBid();
        if (highestBid == null) return getRandomCard(CardMath.findLowestCards(legalCards));

        PlayerId miseriePlayerId = highestBid.playerId();

        if (!memory.hasPlayerActedInCurrentTrick(miseriePlayerId)) {
            return getRandomCard(CardMath.findLowestCards(legalCards));
        }

        boolean isMiseriePlayerWinning = memory.getCurrentWinnerId().equals(miseriePlayerId);

        if (isMiseriePlayerWinning) {
            Card miserieCard = memory.getCardPlayedBy(miseriePlayerId);
            Card lowestLosingCard = CardMath.findLowestLosingCard(legalCards, miserieCard, lead,null);

            return lowestLosingCard != null ? lowestLosingCard : getRandomCard(CardMath.findHighestCards(legalCards));
        }

        return getRandomCard(CardMath.findHighestCards(legalCards));
    }

    // --- Utility Helpers ---

    private Card playLowest(List<Card> options) {
        return getRandomCard(CardMath.findLowestCards(options));
    }

    private Card playHighest(List<Card> options) {
        return getRandomCard(CardMath.findHighestCards(options));
    }

    /** Selects a random card from a list of tied cards to ensure unpredictable bot behavior. */
    private Card getRandomCard(List<Card> tiedCards) {
        Objects.requireNonNull(tiedCards, "Cannot select a random card from  null.");
        if (tiedCards.isEmpty()) {
            throw new IllegalStateException("Cannot select a random card from an empty list.");
        }
        return tiedCards.get(this.random.nextInt(tiedCards.size()));
    }

    private Card findGuaranteedWinningLead(List<Card> cards) {
        for (Card card : cards) {
            if (memory.isHighestUnplayedCardInSuit(card)) {
                return card;
            }
        }
        return null;
    }

    // --- Bidding Helpers ---

    private boolean isLegalBid(BidType intendedBid) {
        if (intendedBid == null) return false;
        if (intendedBid == BidType.PASS) return true;

        BidTurn highestTurn = memory.getHighestBid();
        if (highestTurn == null || highestTurn.bidType() == BidType.PASS) {
            return true;
        }

        BidType highestType = highestTurn.bidType();

        int comparison = intendedBid.compareTo(highestType);
        if (comparison < 0) return false;

        if (intendedBid.getCategory() != BidCategory.MISERIE) {
            return comparison > 0;
        }

        return true;
    }

    private BidType evaluateMiserieEligibility(List<Card> hand) {
        Card highestCard = hand.stream()
                .max(Comparator.comparing(Card::rank))
                .orElseThrow(() ->new IllegalStateException("Hand cannot be empty during miserie evaluation."));

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

    //FIXME: trump
    private Bid mapToHighBid(int tricks, Suit chosenTrump) {
        return switch (tricks) {
            case 9  -> BidType.ABONDANCE_9.instantiate(chosenTrump);
            case 10 -> BidType.ABONDANCE_10.instantiate(chosenTrump);
            case 11 -> BidType.ABONDANCE_11.instantiate(chosenTrump);
            case 12 -> BidType.ABONDANCE_12_OT.instantiate(chosenTrump);
            case 13 -> {
                if (chosenTrump == memory.getCurrentTrump()) {
                    yield BidType.SOLO_SLIM.instantiate(chosenTrump);
                } else {
                    yield BidType.SOLO.instantiate(chosenTrump);
                }
            }
            default -> throw new IllegalArgumentException("Invalid tricks value for mapping to high bid: " + tricks);        };
    }
}