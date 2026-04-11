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

public class SmartBotStrategy implements Strategy {

    private record TrumpEvaluation(Suit suit, int expectedTricks) {}

    // --- Internal Memory ---
    private final SmartBotMemory memory;
    private Player myself;

    public SmartBotStrategy() {
        this.memory = new SmartBotMemory();
    }

    /**
     * Exposes the bot's memory so the Game Engine can register it as an observer.
     */
    public GameObserver getMemoryObserver() {
        return this.memory;
    }

    // --- Public Strategy Methods ---

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

        if (bestEvaluation.expectedTricks() >= 9) {
            return mapToHighBid(player, bestEvaluation.expectedTricks(), bestEvaluation.suit());
        }

        if (tricksWithCurrentTrump >= 3 && memory.hasActiveProposal()) {
            return BidType.ACCEPTANCE.instantiate(player, null);
        } else if (tricksWithCurrentTrump >= 5) {
            return BidType.PROPOSAL.instantiate(player, null);
        }

        return BidType.PASS.instantiate(player, null);
    }

    @Override
    public Card chooseCardToPlay(List<Card> currentHand, Suit lead) {
        // Pure delegation! The strategy asks the tactics to do the heavy lifting.
        PlayTactic currentTactic = determineTactic();
        return currentTactic.chooseCard(currentHand, lead, this.memory, this.myself);
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
            default -> BidType.SOLO_SLIM.instantiate(player, memory.getCurrentTrump());
        };
    }

    // --- Routing Helper ---

    private PlayTactic determineTactic() {
        Bid highestBid = memory.getHighestBid();

        if (highestBid != null && highestBid.getType().getCategory() == BidCategory.MISERIE) {
            if (highestBid.getPlayer().equals(myself)) {
                return new MiserieTactic();
            } else {
                return new AntiMiserieTactic();
            }
        }
        return new NormalTactic();
    }
}