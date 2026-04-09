package base.domain.player;

import base.domain.bid.Bid;
import base.domain.bid.BidCategory;
import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Rank;
import base.domain.card.Suit;
import base.domain.observer.GameObserver;

import java.util.Comparator;
import java.util.List;

public class SmartBotStrategy implements Strategy, GameObserver {

    private record TrumpEvaluation(Suit suit, int expectedTricks) {}

    private enum Behavior { MISERIE, ANTI_MISERIE, NORMAL }

    // --- Internal Memory ---

    private SmartBotMemory memory;
    private Player myself;
    public SmartBotStrategy() {
        this.memory = new SmartBotMemory();
    }

    // --- Strategy Methods ---

    @Override
    public Bid determineBid(Player player) {
        if (player == null) {throw new IllegalArgumentException("Player cannot be null.");}

        this.myself = player;

        BidType miserieBidType = evaluateMiserieEligibility(player.getHand());
        if (miserieBidType != null) {
            return miserieBidType.instantiate(player, null);
        }

        int tricksWithCurrentTrump = estimateWinningTricks(player.getHand(), memory.getCurrentTrump());
        TrumpEvaluation bestEvaluation = findBestTrumpSuit(player.getHand(), tricksWithCurrentTrump);

        // CASE A: 9 to 13 tricks -> Solo / Abondance
        // may choose Trump Suit
        if (bestEvaluation.expectedTricks() >= 9) {
            return mapToHighBid(player, bestEvaluation.expectedTricks(), bestEvaluation.suit());
        }

        // CASE B: 3 to 8 tricks -> Proposal / Acceptance
        // plays with current Trump
        if (tricksWithCurrentTrump >= 3 && memory.hasActiveProposal()) {
            return BidType.ACCEPTANCE.instantiate(player, null);
            } else if (tricksWithCurrentTrump >= 5) {
                return BidType.PROPOSAL.instantiate(player, null);
            }

        // CASE C: <= 2 tricks, or 3-4 tricks with no active proposal -> Pass
        return BidType.PASS.instantiate(player, null);
    }


    @Override
    public Card chooseCardToPlay(List<Card> currentHand, Suit lead) {
        Behavior currentBehavior = determineCurrentBehavior(this.myself);

        return switch (currentBehavior) {
            case MISERIE       -> playMiserieLogic(currentHand, lead);
            case ANTI_MISERIE  -> playAntiMiserieLogic(currentHand, lead);
            case NORMAL        -> playNormalLogic(currentHand, lead);
        };
    }

    @Override
    public boolean requiresConfirmation() {
        return false;
    }

    // --- Private Helpers ---

    private Behavior determineCurrentBehavior(Player myself) {
        Bid highestBid = memory.getHighestBid();

        if (highestBid != null && highestBid.getType().getCategory() == BidCategory.MISERIE) {
            if (highestBid.getPlayer().equals(myself)) {
                return Behavior.MISERIE;
            } else {
                return Behavior.ANTI_MISERIE;
            }
        }
        return Behavior.NORMAL;
    }

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
        Suit bestSuit = memory.getCurrentTrump();
        int maxTricks = tricksWithDealtTrump;

        for (Suit suit : Suit.values()) {
            int tricks = estimateWinningTricks(hand, suit);
            if (tricks > maxTricks) {
                maxTricks = tricks;
                bestSuit = suit;
            }
        }
        return new SmartBotStrategy.TrumpEvaluation (bestSuit, maxTricks);
    }

    private Bid mapToHighBid(Player player, int tricks, Suit chosenTrump) {
        return switch (tricks) {
            case 9  -> BidType.ABONDANCE_9.instantiate(player, chosenTrump);
            case 10 -> BidType.ABONDANCE_10.instantiate(player, chosenTrump);
            case 11 -> BidType.ABONDANCE_11.instantiate(player, chosenTrump);
            case 12 -> BidType.ABONDANCE_12_OT.instantiate(player, chosenTrump);
            default -> BidType.SOLO_SLIM.instantiate(player, memory.getCurrentTrump());
        };
    }

    private Card playMiserieLogic(List<Card> currentHand, Suit lead) {
        return null; // TODO
    }

    private Card playAntiMiserieLogic(List<Card> currentHand, Suit lead) {
        return null; // TODO
    }

    private Card playNormalLogic(List<Card> currentHand, Suit lead) {
        if (memory.isLeadPlayer()) {
            // "Play highest card in a suit if you have it, else lowest random"
        } else if (isMyTeamWinning()) {
                // "Play lowest legal card"
        } else {
                // "Play highest in lead suit, or lowest trump"
        }

        return null; // fallback
    }

    private boolean isMyTeamWinning() {
        return  memory.getBidTeam(myself).contains(memory.getWinningPLayer());
    }
}