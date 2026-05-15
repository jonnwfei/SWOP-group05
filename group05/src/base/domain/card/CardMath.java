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
     * Determines which of two cards is strictly stronger according to Whist trick-taking rules.
     * This is a context-free mathematical evaluation and does not assume either card is currently winning.
     *
     * @param challenger The card attempting to win.
     * @param incumbent  The card currently holding priority (or null if first card).
     * @param leadSuit   The suit led in the trick.
     * @param trumpSuit  The active trump suit (null if No-Trump / Miserie).
     * @return {@code true} if the challenger mathematically beats the incumbent, {@code false} otherwise.
     * @throws NullPointerException if the challenger card is null.
     */
    public static boolean doesCardBeat(Card challenger, Card incumbent, Suit leadSuit, Suit trumpSuit) {
        Objects.requireNonNull(challenger, "Challenger card cannot be null.");

        if (incumbent == null) {
            return true;
        }

        Suit cSuit = challenger.suit();
        Suit iSuit = incumbent.suit();

        // Same suit: The highest rank always wins.
        // (cleanly handles Trump vs Trump, Lead vs Lead, and Off-suit vs Off-suit)
        if (cSuit == iSuit) {
            return challenger.rank().compareTo(incumbent.rank()) > 0;
        }

        if (cSuit == trumpSuit) return true;

        if (iSuit == trumpSuit) return false;

        // Neither are trumps, and they are different suits.
        return cSuit == leadSuit;
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
     * @param leadSuit   The suit led in the trick.
     * @param trump        The active trump suit (null if No-Trump).
     * @return The highest losing card, or null if all available options will win the trick.
     */
    public static Card findHighestLosingCard(List<Card> legalCards, Card cardToLoseTo, Suit leadSuit, Suit trump) {
        if (legalCards == null || legalCards.isEmpty()) return null;

        return legalCards.stream()
                .filter(Objects::nonNull)
                .filter(c -> !doesCardBeat(c, cardToLoseTo, lead, trump))
                .max(Comparator.comparing(Card::rank))
                .orElse(null);
    }

    /**
     * Finds the lowest ranked card from a list of options that will strictly LOSE
     * to a specific target card.
     *
     * @param options      The legal cards the player can choose from.
     * @param cardToLoseTo The card the player is trying to avoid beating.
     * @param leadSuit   The suit led in the trick.
     * @param trump        The active trump suit (null if No-Trump).
     * @return The lowest losing card, or null if all available options will win the trick.
     */
    public static Card findLowestLosingCard(List<Card> options, Card cardToLoseTo, Suit leadSuit, Suit trump) {
        if (options == null || options.isEmpty()) return null;

        return options.stream()
                .filter(Objects::nonNull)
                .filter(c -> !doesCardBeat(c, cardToLoseTo, lead, trump))
                .min(Comparator.comparing(Card::rank))
                .orElse(null);
    }
}