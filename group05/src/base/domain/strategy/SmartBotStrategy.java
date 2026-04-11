package base.domain.strategy;

import base.domain.bid.Bid;
import base.domain.bid.BidCategory;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.observer.GameObserver;
import base.domain.player.*;

import java.util.Comparator;
import java.util.List;

public final class SmartBotStrategy implements Strategy {

    private record TrumpEvaluation(Suit suit, int expectedTricks) {}
    private static final int MIN_TRICKS_FOR_ACCEPTANCE = 3;
    private static final int MIN_TRICKS_FOR_PROPOSAL = 5;
    private static final int MIN_TRICKS_FOR_ABONDANCE = 9;

    private enum PlayTactic {
        NORMAL,
        MISERIE,
        ANTI_MISERIE
    }

    // --- Internal Memory ---
    private PlayTactic currentPlayTactic;
    private final SmartBotMemory memory;
    private final PlayerId myself;

    public SmartBotStrategy(PlayerId myself) {
        if (myself == null) {throw new IllegalArgumentException("PlayerId can't be null");}
        this.myself = myself;
        this.memory = new SmartBotMemory();
        this.currentPlayTactic = PlayTactic.NORMAL;
    }

    /**
     * Exposes the bot's memory so the Game Engine can register it as an observer.
     */
    public GameObserver getGameObserver() {
        return this.memory;
    }

    // --- Public Strategy Methods ---

    @Override
    public Bid determineBid(Player player) {
        if (player == null) {throw new IllegalArgumentException("Player cannot be null.");}

        BidType miserieBidType = evaluateMiserieEligibility(player.getHand());
        if (miserieBidType != null) {
            return miserieBidType.instantiate(player, null);
        }

        int tricksWithCurrentTrump = estimateWinningTricks(player.getHand(), memory.getCurrentTrump());
        TrumpEvaluation bestEvaluation = findBestTrumpSuit(player.getHand(), tricksWithCurrentTrump);

        if (bestEvaluation.expectedTricks() >= MIN_TRICKS_FOR_ABONDANCE) {
            return mapToHighBid(player, bestEvaluation.expectedTricks(), bestEvaluation.suit());
        }

        if (tricksWithCurrentTrump >= MIN_TRICKS_FOR_ACCEPTANCE && memory.hasActiveProposal()) {
            return BidType.ACCEPTANCE.instantiate(player, null);
        } else if (tricksWithCurrentTrump >= MIN_TRICKS_FOR_PROPOSAL) {
            return BidType.PROPOSAL.instantiate(player, null);
        }

        return BidType.PASS.instantiate(player, null);
    }

    @Override
    public Card chooseCardToPlay(List<Card> currentHand, Suit lead) {
        // Always ensure the tactic is up-to-date before picking a card
        updateCurrentTactic();

        return switch (currentPlayTactic) {
            case NORMAL        -> playNormalLogic(currentHand, lead);
            case MISERIE       -> playMiserieLogic(currentHand, lead);
            case ANTI_MISERIE  -> playAntiMiserieLogic(currentHand, lead);
        };
    }

    @Override
    public boolean requiresConfirmation() {
        return false;
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

    private Bid mapToHighBid(Player player, int tricks, Suit chosenTrump) {
        return switch (tricks) {
            case 9  -> BidType.ABONDANCE_9.instantiate(player, chosenTrump);
            case 10 -> BidType.ABONDANCE_10.instantiate(player, chosenTrump);
            case 11 -> BidType.ABONDANCE_11.instantiate(player, chosenTrump);
            case 12 -> BidType.ABONDANCE_12_OT.instantiate(player, chosenTrump);
            case 13 -> {
                if (chosenTrump == memory.getCurrentTrump()) {
                    yield BidType.SOLO_SLIM.instantiate(player, chosenTrump);
                } else {
                    yield BidType.SOLO.instantiate(player, chosenTrump);
                }
            }
            default -> throw new IllegalArgumentException("Invalid tricks value");
        };
    }

    // --- State Routing Helper ---

    private void updateCurrentTactic() {
        Bid highestBid = memory.getHighestBid();
        if (highestBid != null && highestBid.getType().getCategory() == BidCategory.MISERIE) {
            if (highestBid.getPlayer().getId().equals(myself)) {
                this.currentPlayTactic = PlayTactic.MISERIE;
            } else {
                this.currentPlayTactic = PlayTactic.ANTI_MISERIE;
            }
        } else {
            this.currentPlayTactic = PlayTactic.NORMAL;
        }
    }

    // --- Playing Helpers ---

    private Card playNormalLogic(List<Card> currentHand, Suit lead) {
        List<Card> legalCards = getLegalCards(currentHand, lead);

        if (memory.isLeadPlayer()) {
            return findHighestCard(legalCards);
        }

        if (memory.isTeamWinning(myself)) {
            return findLowestCard(legalCards);
        } else {
            Card lowestTrump = findLowestTrump(legalCards, memory.getCurrentTrump());
            if (lowestTrump != null) {
                return lowestTrump;
            }
            return findHighestCard(legalCards);
        }
    }

    private Card playMiserieLogic(List<Card> currentHand, Suit lead) {
        List<Card> legalCards = getLegalCards(currentHand, lead);

        if (memory.isLeadPlayer()) {
            return findLowestCard(legalCards);
        }

        var winningTurn = memory.getCurrentWinningTurn();
        Card currentWinningCard = winningTurn != null ? winningTurn.playedCard() : null;

        Card highestSafeCard = legalCards.stream()
                .filter(c -> !doesCardBeat(c, currentWinningCard, lead, null))
                .max(Comparator.comparing(Card::rank))
                .orElse(null);

        if (highestSafeCard != null) {
            return highestSafeCard;
        } else {
            return findHighestCard(legalCards);
        }
    }

    private Card playAntiMiserieLogic(List<Card> currentHand, Suit lead) {
        List<Card> legalCards = getLegalCards(currentHand, lead);
        PlayerId miseriePlayerId = memory.getHighestBid().getPlayer().getId();

        boolean amIPlayingBeforeEnemy = !memory.hasPlayerActedInCurrentTrick(miseriePlayerId);

        if (amIPlayingBeforeEnemy) {
            return findLowestCard(legalCards);
        } else {
            PlayerId currentWinnerId = memory.calculateCurrentWinnerId();

            if (miseriePlayerId.equals(currentWinnerId)) {
                Card enemyCard = memory.getCardPlayedBy(miseriePlayerId);
                Card lowestSafeCard = legalCards.stream()
                        .filter(c -> !doesCardBeat(c, enemyCard, lead, null))
                        .min(Comparator.comparing(Card::rank))
                        .orElse(null);

                if (lowestSafeCard != null) {
                    return lowestSafeCard;
                } else {
                    return findHighestCard(legalCards);
                }
            } else {
                return findHighestCard(legalCards);
            }
        }
    }

    // --- Internal Math & Filtering Tools ---

    private List<Card> getLegalCards(List<Card> hand, Suit lead) {
        if (lead == null) return hand;

        List<Card> followingCards = hand.stream()
                .filter(card -> card.suit() == lead)
                .toList();

        return followingCards.isEmpty() ? hand : followingCards;
    }

    private Card findLowestCard(List<Card> cards) {
        return cards.stream().min(Comparator.comparing(Card::rank)).orElse(null);
    }

    private Card findHighestCard(List<Card> cards) {
        return cards.stream().max(Comparator.comparing(Card::rank)).orElse(null);
    }

    private Card findLowestTrump(List<Card> cards, Suit trumpSuit) {
        return cards.stream()
                .filter(c -> c.suit() == trumpSuit)
                .min(Comparator.comparing(Card::rank))
                .orElse(null);
    }

    private boolean doesCardBeat(Card challenger, Card currentBest, Suit leadSuit, Suit trumpSuit) {
        if (currentBest == null) return true;

        boolean isChallengerTrump = (trumpSuit != null && challenger.suit() == trumpSuit);
        boolean isBestTrump = (trumpSuit != null && currentBest.suit() == trumpSuit);

        if (isChallengerTrump) {
            return !isBestTrump || challenger.rank().compareTo(currentBest.rank()) > 0;
        } else if (!isBestTrump && challenger.suit() == leadSuit) {
            return challenger.rank().compareTo(currentBest.rank()) > 0;
        }
        return false;
    }
}