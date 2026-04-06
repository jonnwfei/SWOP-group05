package base.domain.player;

import base.domain.bid.Bid;
import base.domain.bid.BidCategory;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.deck.Deck;
import base.domain.observer.GameObserver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SmartBotStrategy implements Strategy, GameObserver {

    private enum Behavior {
        MISERIE,
        ANTI_MISERIE,
        NORMAL
    }

    // --- Internal State (Memory) ---

    private Behavior currentBehavior = Behavior.NORMAL;
    private Suit currentTrump = null;
    private final List<Card> unplayedCards = new Deck().getCards();
    private final List<Bid> bidsMemory = new ArrayList<>();

    // --- Strategy Methods ---

    @Override
    public Bid determineBid(Player player) {
        BidType miserieBidType = evaluateMiserieEligibility(player.getHand());
        if (miserieBidType != null) {
            return miserieBidType.instantiate(player, null);
        }

        int tricksWithDealtTrump = estimateWinningTricks(player.getHand(), currentTrump);
        TrumpEvaluation bestEvaluation = findBestTrumpSuit(player.getHand(), tricksWithDealtTrump);

        // CASE A: 9 to 13 tricks -> Solo / Abondance
        if (bestEvaluation.expectedTricks() >= 9) {
            return mapToHighBid(player, bestEvaluation.expectedTricks(), bestEvaluation.suit());
        }

        // CASE B: 3 to 8 tricks -> Proposal / Acceptance
        if (tricksWithDealtTrump >= 3) {
            boolean isProposalActive = this.bidsMemory.stream()
                    .anyMatch(bid -> bid.getType() == BidType.PROPOSAL);

            if (isProposalActive) {
                return BidType.ACCEPTANCE.instantiate(player, null);
            } else if (tricksWithDealtTrump >= 5) {
                return BidType.PROPOSAL.instantiate(player, null);
            }
        }

        // CASE C: <= 2 tricks, or 3-4 tricks with no active proposal -> Pass
        return BidType.PASS.instantiate(player, null);
    }

    @Override
    public Card chooseCardToPlay(List<Card> currentHand, Suit lead) {
        return switch (this.currentBehavior) {
            case MISERIE       -> playMiserieLogic(currentHand, lead);
            case ANTI_MISERIE  -> playAntiMiserieLogic(currentHand, lead);
            case NORMAL        -> playNormalLogic(currentHand, lead);
        };
    }

    @Override
    public boolean requiresConfirmation() {
        return false;
    }

    // --- Observer Methods (Updating Memory) ---

    @Override
    public void onRoundStarted() {
        this.currentBehavior = Behavior.NORMAL;
        this.currentTrump = null;
        this.bidsMemory.clear();
        this.unplayedCards.clear();
        this.unplayedCards.addAll(new Deck().getCards()); // Reset to a full 52-card deck
    }

    @Override
    public void onTrumpDetermined(Suit trumpSuit) {
        this.currentTrump = trumpSuit;
    }

    @Override
    public void onBidPlaced(Bid bid) {
        this.bidsMemory.add(bid);

        // Dynamically shift behavior if someone plays Miserie
        if (bid.getType().getCategory() == BidCategory.MISERIE) {
            //TODO: problem, how to differentiate this player from others?
            this.currentBehavior = Behavior.ANTI_MISERIE;
        }
    }

    @Override
    public void onCardPlayed(Card card) {
        // Remove the card from the unplayed tracker so the bot knows it's gone
        this.unplayedCards.remove(card);
    }

    // --- Private Helpers ---

    private BidType evaluateMiserieEligibility(List<Card> hand) {
        Card highestCard = hand.stream()
                .max(Comparator.comparing(Card::rank))
                .orElseThrow(() -> new IllegalArgumentException("Hand is empty"));

        if (highestCard.rank().compareTo(Rank.SEVEN) <= 0) {
            return BidType.OPEN_MISERIE;
        }
        if (highestCard.rank().compareTo(Rank.TEN) <= 0) {
            return BidType.MISERIE;
        }
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
        Suit bestSuit = this.currentTrump;
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
            default -> BidType.SOLO_SLIM.instantiate(player, this.currentTrump);
        };
    }

    private Card playMiserieLogic(List<Card> currentHand, Suit lead) {
        return null; // TODO
    }

    private Card playAntiMiserieLogic(List<Card> currentHand, Suit lead) {
        return null; // TODO
    }

    private Card playNormalLogic(List<Card> currentHand, Suit lead) {
        return null; // TODO
    }

    private record TrumpEvaluation(Suit suit, int expectedTricks) {}
}