import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author John Cai
 * @since 25/02/2026
 */
public class Trick {
    private final Suit trumpSuit;
    private final Player startingPlayer;
    private Player winningPlayer;
    private LinkedHashMap<Player, Card> playedCards;

    /**
     * @param startingPlayer starting player of this trick
     * @throws Error if Trick constructor is called without a starting player
     */
    public Trick(Player startingPlayer, Suit trumpSuit) {
        if (startingPlayer == null) {
            throw new IllegalArgumentException("Trick: Starting player must exist, cannot be null");
        }
        if (trumpSuit == null) {
            throw new IllegalArgumentException("Trick: Trump suit must exist, cannot be null");
        }
        this.trumpSuit = trumpSuit;
        this.startingPlayer = startingPlayer;
        this.winningPlayer = null;
        this.playedCards = new LinkedHashMap<>();
    }


    /**
     * @return Player starting player
     */
    public Player getStartingPlayer() {
        return this.startingPlayer;
    }

    /**
     * @return player winning player
     */
    public Player getWinningPlayer() {
        return this.winningPlayer;
    }

    /**
     * @param player that plays a card
     * @param card   to be played
     * @throws IllegalArgumentException if Player tries to play a card not in their hand
     * @throws IllegalArgumentException if the same Player tries to play more than once in the same
     *                                  trick
     * @throws IllegalArgumentException if Player doesn't follow the leadingSuit when he's able to
     */
    public void playCard(Player player, Card card) {
        List<Card> playerHand = player.getHand();
        if (!playerHand.contains(card)) {
            throw new IllegalArgumentException("Trick: Player does not have this card in their hand");
        }
        if (playedCards.containsKey(player)) {
            throw new IllegalArgumentException("Trick: Player already played in this trick");
        }
        if (!playedCards.isEmpty()) { // Cards have been played, get leading suit
            Suit leadingSuit = getLeadingSuit();

            if (card.getSuit() != leadingSuit && player.hasSuit(leadingSuit)) {
                throw new IllegalArgumentException(
                        "Trick: Illegal move: You must follow the leading suit (" + leadingSuit + ").");
            }
        }

        if (playedCards.size() > 4) {
            throw new IllegalArgumentException("Trick: Trick has already 4 cards");
        }
        player.removeCard(card);
        playedCards.put(player, card);
        if (playedCards.size() == 4) {
            determineWinner();
        }
    }

    /**
     * Determines the winning player of this trick
     */
    private void determineWinner() {
        Suit leadingSuit = getLeadingSuit();

        Player currentWinner = null;
        Card bestCard = null;

        for (Map.Entry<Player, Card> entry : playedCards.entrySet()) {
            Player player = entry.getKey();
            Card playedCard = entry.getValue();

            if (bestCard == null) {
                currentWinner = player;
                bestCard = playedCard;
                continue;
            }

            boolean isNewCardTrump = (playedCard.getSuit() == this.trumpSuit);
            boolean isBestCardTrump = (bestCard.getSuit() == this.trumpSuit);

            if (isNewCardTrump) {
                if (!isBestCardTrump) { // If new Card is Trump it automatically beats all non-trumps
                    currentWinner = player;
                    bestCard = playedCard;
                } else {                // If both TrumpCards, then highest trump wins
                    if (playedCard.getRank().compareTo(bestCard.getRank()) > 0) {
                        currentWinner = player;
                        bestCard = playedCard;
                    }
                }
            } else {
                if (!isBestCardTrump) { // If neither are TrumpCards, highest rank wins
                    if (playedCard.getSuit() == leadingSuit && playedCard.getRank().compareTo(bestCard.getRank()) > 0) {
                        currentWinner = player;
                        bestCard = playedCard;
                    }
                }
            }
        }
        this.winningPlayer = currentWinner;
    }

    private Suit getLeadingSuit() {
        Card firstCard = playedCards.values().iterator().next();
        return firstCard.getSuit();
    }
}
