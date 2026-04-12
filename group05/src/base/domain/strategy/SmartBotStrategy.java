package base.domain.strategy;

import base.domain.bid.Bid;
import base.domain.bid.BidCategory;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.observer.GameObserver;
import base.domain.player.*;
import base.domain.turn.BidTurn;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

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
    private final Random random;

    public SmartBotStrategy(PlayerId myself) {
        if (myself == null) {throw new IllegalArgumentException("PlayerId can't be null");}
        this.myself = myself;
        this.memory = new SmartBotMemory();
        this.currentPlayTactic = PlayTactic.NORMAL;
        this.random = new Random();
    }

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
        updateCurrentTactic();
        List<Card> legalCards = getLegalCards(currentHand, lead);

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

    private Card playToWinTrick(List<Card> legalCards, Suit lead) {
        if (memory.isLeadPlayer()) {
            Card guaranteedWinner = findGuaranteedWinningLead(legalCards);
            return guaranteedWinner != null ? guaranteedWinner : findLowestCard(legalCards);
        }

        if (memory.isTeamWinning(myself)) {
            return findLowestCard(legalCards); // Conserve high cards if partner is winning
        } else {
            Suit trump = memory.getCurrentTrump();
            boolean isVoidInLead = lead != null && legalCards.stream().noneMatch(c -> c.suit() == lead);

            if (isVoidInLead) {
                Card lowestTrump = findLowestTrump(legalCards, trump);
                if (lowestTrump != null) return lowestTrump;

                // If you can't follow lead and no trumps, discard the lowest possible card
                return findLowestCard(legalCards);
            }

            // If we must follow suit, play the highest to try and win
            return findHighestCard(legalCards);
        }
    }

    private Card playToLoseTrick(List<Card> legalCards, Suit lead) {
        if (memory.isLeadPlayer()) {
            return findLowestCard(legalCards);
        }

        Card currentWinningCard = getWinningCard();
        Card highestSafe = findHighestSafeCard(legalCards, currentWinningCard, lead, null);

        // Play the highest card that doesn't win. If all cards win, dump absolute highest card.
        return highestSafe != null ? highestSafe : findHighestCard(legalCards);
    }

    private Card playToForceMiserieLoss(List<Card> legalCards, Suit lead) {
        PlayerId miseriePlayerId = memory.getHighestBid().playerId();

        if (!memory.hasPlayerActedInCurrentTrick(miseriePlayerId)) {
            // We play before the Miserie player
            return findLowestCard(legalCards);
        }

        // We play after the Miserie player
        if (miseriePlayerId.equals(memory.calculateCurrentWinnerId())) {
            Card miserieCard = memory.getCardPlayedBy(miseriePlayerId);

            // Try to play our lowest card that keeps the Miserie player winning
            Card lowestSafeCard = findLowestSafeCard(legalCards, miserieCard, lead, null);
            return lowestSafeCard != null ? lowestSafeCard : findHighestCard(legalCards);
        } else {
                // Miserie player is currently safe, play the highest legal card
            return findHighestCard(legalCards);
        }
    }

    // --- Card Filtering & Math Tools (Pure Fabrication) ---

    private List<Card> getLegalCards(List<Card> hand, Suit lead) {
        if (lead == null) return hand;

        List<Card> followingCards = hand.stream()
                .filter(card -> card.suit() == lead)
                .toList();

        return followingCards.isEmpty() ? hand : followingCards;
    }

    private Card findHighestSafeCard(List<Card> options, Card cardToLoseTo, Suit lead, Suit trump) {
        return options.stream()
                .filter(c -> !doesCardBeat(c, cardToLoseTo, lead, trump))
                .max(Comparator.comparing(Card::rank))
                .orElse(null);
    }

    private Card findLowestSafeCard(List<Card> options, Card cardToLoseTo, Suit lead, Suit trump) {
        return options.stream()
                .filter(c -> !doesCardBeat(c, cardToLoseTo, lead, trump))
                .min(Comparator.comparing(Card::rank))
                .orElse(null);
    }

    private Card findLowestCard(List<Card> cards) {
        if (cards.isEmpty()) return null;
        Rank lowestRank = cards.stream().min(Comparator.comparing(Card::rank)).get().rank();
        List<Card> lowestCards = cards.stream().filter(c -> c.rank() == lowestRank).toList();
        return lowestCards.get(random.nextInt(lowestCards.size()));
    }

    private Card findHighestCard(List<Card> cards) {
        return cards.stream().max(Comparator.comparing(Card::rank)).orElse(null);
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
}