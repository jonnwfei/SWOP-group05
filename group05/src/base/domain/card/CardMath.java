package base.domain.card;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * A Pure Fabrication utility class responsible for stateless mathematical and
 * logical evaluations of Cards.
 * <p>
 *
 * @author Tommy Wu, Stan Kestens
 * @since 01/03/2026
 */
public final class CardMath {

    // Private constructor to prevent instantiation of a utility class
    private CardMath() {}

    /**
     * Filters a hand to return only the cards that are legally allowed to be played.
     * According to Whist rules, a player must follow the lead suit if possible.
     *
     * @param hand The player's current hand.
     * @param lead The suit led in the current trick. Can be null if the player is leading.
     * @return A list of legally playable cards.
     * @throws NullPointerException if the hand is null.
     */
    public static List<Card> getLegalCards(List<Card> hand, Suit lead) {
        Objects.requireNonNull(hand, "Hand cannot be null.");
        if (lead == null) return hand; // First to play can play any card

        List<Card> followingCards = hand.stream()
                .filter(card -> card != null && card.suit() == lead)
                .toList();

        // If the player's legal hand is empty in the lead suit, they can play any card
        return followingCards.isEmpty() ? hand : followingCards;
    }

    /**
     * Finds all cards in a list that share the highest numerical rank present in that list.
     *
     * @param cards The list of cards to evaluate.
     * @return A list of the highest ranked cards, or an empty list if the input is empty.
     */
    public static List<Card> findHighestCards(List<Card> cards) {
        if (cards == null || cards.isEmpty()) return List.of();

        Rank highestRank = cards.stream()
                .filter(Objects::nonNull)
                .map(Card::rank)
                .max(Rank::compareTo)
                .orElse(null);

        if (highestRank == null) return List.of();

        return cards.stream()
                .filter(c -> c != null && c.rank() == highestRank)
                .toList();
    }

    /**
     * Finds all cards in a list that share the lowest numerical rank present in that list.
     *
     * @param cards The list of cards to evaluate.
     * @return A list of the lowest ranked cards, or an empty list if the input is empty.
     */
    public static List<Card> findLowestCards(List<Card> cards) {
        if (cards == null || cards.isEmpty()) return List.of();

        Rank lowestRank = cards.stream()
                .filter(Objects::nonNull)
                .map(Card::rank)
                .min(Rank::compareTo)
                .orElse(null);

        if (lowestRank == null) return List.of();

        return cards.stream()
                .filter(c -> c != null && c.rank() == lowestRank)
                .toList();
    }

    /**
     * Determines if a newly played card beats the currently winning card in a trick.
     *
     * @param challenger  The newly played card being evaluated.
     * @param currentBest The card currently winning the trick. If null, challenger automatically wins.
     * @param trumpSuit   The active trump suit for the round (null if playing a No-Trump bid).
     * @return {@code true} if the challenger beats the current best, {@code false} otherwise.
     * @throws NullPointerException if the challenger card is null.
     */
    public static boolean doesCardBeat(Card challenger, Card currentBest, Suit trumpSuit) {
        Objects.requireNonNull(challenger, "Challenger card cannot be null.");

        // If it's the first card played, it automatically wins
        if (currentBest == null) {
            return true;
        }

        Suit challengerSuit = challenger.suit();
        Suit currentBestSuit = currentBest.suit();

        // 1. Suits match: The highest rank wins.
        if (challengerSuit == currentBestSuit) {
            return challenger.rank().compareTo(currentBest.rank()) > 0;
        }

        // 2. Suits differ: Challenger only wins if it is a Trump card.
        return challengerSuit == trumpSuit;
    }

    /**
     * Finds the highest ranked card of a specific suit within a list of cards.
     *
     * @param cards The list of cards to search.
     * @param suit  The target suit to filter by.
     * @return The highest card of the specified suit, or null if no cards of that suit are present.
     * @throws NullPointerException if the cards list or suit is null.
     */
    public static Card findHighestCardOfSuit(List<Card> cards, Suit suit) {
        Objects.requireNonNull(cards, "Cards list cannot be null.");
        Objects.requireNonNull(suit, "Suit cannot be null.");

        return cards.stream()
                .filter(c -> c != null && c.suit() == suit)
                .max(Comparator.comparing(Card::rank))
                .orElse(null);
    }

    /**
     * Finds the lowest ranked card of a specific suit within a list of cards.
     *
     * @param cards The list of cards to search.
     * @param suit  The target suit to filter by.
     * @return The lowest card of the specified suit, or null if no cards of that suit are present.
     * @throws NullPointerException if the cards list or suit is null.
     */
    public static Card findLowestCardOfSuit(List<Card> cards, Suit suit) {
        Objects.requireNonNull(cards, "Cards list cannot be null.");
        Objects.requireNonNull(suit, "Suit cannot be null.");

        return cards.stream()
                .filter(c -> c != null && c.suit() == suit)
                .min(Comparator.comparing(Card::rank))
                .orElse(null);
    }

    /**
     * Finds the highest ranked card from a list of options that will strictly LOSE
     * to a specific target card.
     *
     * @param legalCards      The legal cards the player can choose from.
     * @param cardToLoseTo The card the player is trying to avoid beating.
     * @param trump        The active trump suit (null if No-Trump).
     * @return The highest losing card, or null if all available options will win the trick.
     */
    public static Card findHighestLosingCard(List<Card> legalCards, Card cardToLoseTo, Suit trump) {
        if (legalCards == null || legalCards.isEmpty()) return null;

        return legalCards.stream()
                .filter(Objects::nonNull)
                .filter(c -> !doesCardBeat(c, cardToLoseTo, trump))
                .max(Comparator.comparing(Card::rank))
                .orElse(null);
    }

    /**
     * Finds the lowest ranked card from a list of options that will strictly LOSE
     * to a specific target card.
     *
     * @param options      The legal cards the player can choose from.
     * @param cardToLoseTo The card the player is trying to avoid beating.
     * @param trump        The active trump suit (null if No-Trump).
     * @return The lowest losing card, or null if all available options will win the trick.
     */
    public static Card findLowestLosingCard(List<Card> options, Card cardToLoseTo, Suit trump) {
        if (options == null || options.isEmpty()) return null;

        return options.stream()
                .filter(Objects::nonNull)
                .filter(c -> !doesCardBeat(c, cardToLoseTo, trump))
                .min(Comparator.comparing(Card::rank))
                .orElse(null);
    }
}