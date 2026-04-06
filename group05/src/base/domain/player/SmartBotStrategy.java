package base.domain.player;

import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.deck.Deck;

import java.util.Comparator;
import java.util.List;

public class SmartBotStrategy implements Strategy {

    private enum behavior {
        MISERIE,
        ANTI_MISERIE,
        NORMAL
    }

    private behavior currentBehavior = behavior.NORMAL;
    private final Suit currentTrump = null;
    private final List<Card> unplayedCards = new Deck().getCards();

    @Override
    public Bid determineBid(Player player) {
        BidType miserieBidType = evaluateMiserieEligibility(player.getHand());
        if (miserieBidType != null) {
            return miserieBidType.instantiate(player, null);
        }
        
        int tricksWithCurrentTrump = estimateWinningTricks(player.getHand(), currentTrump);

        TrumpEvaluation bestEvaluation = findBestTrumpSuit(player.getHand(), tricksWithCurrentTrump);
        
        // CASE A: 9 to 13 tricks -> Solo / Abondance 
        if (bestEvaluation.expectedTricks >= 9) {
            return mapToHighBid(player, bestEvaluation.expectedTricks, bestEvaluation.suit);
        }

        // CASE B: 3 to 8 tricks -> Proposal / Acceptance
        //TODO

        // CASE C: <= 2 tricks, or 3-4 tricks with no active proposal -> Pass
        return BidType.PASS.instantiate(player, null);
    }

    /**
     * Checks if the hand qualifies for Miserie or Open Miserie.
     */
    private BidType evaluateMiserieEligibility(List<Card> hand) {
        Card highestCard = hand.stream()
                .max(Comparator.comparing(Card::rank))
                .orElseThrow(() -> new IllegalArgumentException("Hand is empty"));

        // No card higher than 7
        if (highestCard.rank().compareTo(Rank.SEVEN) <= 0) {
            return BidType.OPEN_MISERIE;
        }
        // No card higher than 10
        if (highestCard.rank().compareTo(Rank.TEN) <= 0) {
            return BidType.MISERIE;
        }
        
        return null;
    }

    /**
     * Calculates the heuristic for expected tricks based on a specific trump suit.
     */
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

    /**
     * Evaluates all 4 suits to find the one that yields the highest expected tricks.
     */
    private TrumpEvaluation findBestTrumpSuit(List<Card> hand, int tricksWithDealtTrump) {
        Suit bestSuit = null;
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

    /**
     * Maps a high trick count (9-13) to the corresponding high-value bid.
     */
    private Bid mapToHighBid(Player player, int tricks, Suit chosenTrump) {
        return switch (tricks) {
            case 9  -> BidType.ABONDANCE_9.instantiate(player, chosenTrump);
            case 10 -> BidType.ABONDANCE_10.instantiate(player, chosenTrump);
            case 11 -> BidType.ABONDANCE_11.instantiate(player, chosenTrump);
            case 12 -> BidType.ABONDANCE_12.instantiate(player, chosenTrump);
            // Solo Slim requires all 13 tricks and forces the dealt trump (knownTrump)
            default -> BidType.SOLO_SLIM.instantiate(player, this.currentTrump);
        };
    }

    @Override
    public Card chooseCardToPlay(List<Card> currentHand, Suit lead) {
        return switch (this.currentBehavior) {
            case MISERIE       -> playMiserieLogic(currentHand, lead);
            case ANTI_MISERIE  -> playAntiMiserieLogic(currentHand, lead);
            case NORMAL        -> playNormalLogic(currentHand, lead);
        };
    }

    private Card playMiserieLogic(List<Card> currentHand, Suit lead) {
        return null;
    }

    private Card playAntiMiserieLogic(List<Card> currentHand, Suit lead) {
        return null;
    }

    private Card playNormalLogic(List<Card> currentHand, Suit lead) {
        return null;
    }

    @Override
    public boolean requiresConfirmation() {
        return false;
    }

    private record TrumpEvaluation(Suit suit, int expectedTricks) {}
}