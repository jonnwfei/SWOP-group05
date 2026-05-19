package base.domain.card;

import java.util.Comparator;
import java.util.List;


public final class CardMath {

    private CardMath() {}

    public static List<Card> getLegalCards(List<Card> hand, Suit lead) {
        if (lead == null) return hand;

        List<Card> followingCards = hand.stream()
                .filter(card -> card.suit() == lead)
                .toList();

        return followingCards.isEmpty() ? hand : followingCards;
    }

    public static List<Card> findHighestCards(List<Card> cards) {
        if (cards == null || cards.isEmpty()) return List.of();

        Rank highestRank = cards.stream()
                .max(Comparator.comparing(Card::rank))
                .get().rank();

        return cards.stream()
                .filter(c -> c.rank() == highestRank)
                .toList();
    }

    public static List<Card> findLowestCards(List<Card> cards) {
        if (cards == null || cards.isEmpty()) return List.of();

        Rank lowestRank = cards.stream()
                .min(Comparator.comparing(Card::rank))
                .get().rank();

        return cards.stream()
                .filter(c -> c.rank() == lowestRank)
                .toList();
    }

    public static boolean doesCardBeat(Card challenger, Card currentBest, Suit leadSuit, Suit trumpSuit) {
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

    /** Returns the highest rank of a given suit, or null if the player is void in that suit */
    public static Rank getHighestRankOfSuit(Suit suit, List<Card> cards) {
        if (suit == null) throw new IllegalArgumentException("suit can't be null.");
        if (cards == null) throw new IllegalArgumentException("cards can't be null.");

        return cards.stream()
                .filter(c -> c.suit() == suit)
                .map(Card::rank)
                .max(base.domain.card.Rank::compareTo)
                .orElse(null);
    }
}
