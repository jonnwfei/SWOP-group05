package base.domain.trick;

import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.card.Card;

import java.util.ArrayList;
import java.util.List;

/**
 * The type Trick.
 *
 * @author John Cai
 * @since 25/02/2026
 */
public class Trick {
    private final Suit trumpSuit;
    private final Player startingPlayer;
    private Player winningPlayer;
    private final List<Turn> turns;

    /**
     * Instantiates a new Trick.
     *
     * @param startingPlayer starting player of this trick
     * @param trumpSuit      the trump suit
     * @throws IllegalArgumentException if Trick constructor is called without a starting player
     */
    public Trick(Player startingPlayer, Suit trumpSuit) {
        if (startingPlayer == null)
            throw new IllegalArgumentException("Trick: Starting player must exist, cannot be null");

        this.trumpSuit = trumpSuit;
        this.startingPlayer = startingPlayer;
        this.winningPlayer = null;
        this.turns = new ArrayList<>();
    }


    /**
     * Gets starting player.
     *
     * @return Player starting player
     */
    public Player getStartingPlayer() {
        return this.startingPlayer;
    }

    /**
     * Gets winning player.
     *
     * @return player winning player
     */
    public Player getWinningPlayer() {
        return this.winningPlayer;
    }

    /**
     * Given player plays the given playedCard and checks whether the play is valid or not.
     * I.e.
     *
     * @param player     that plays a card
     * @param playedCard to be played
     * @throws IllegalArgumentException if the same Player tries to play more than once in the same trick
     */
    public void playCard(Player player, Card playedCard) {
        if (player == null) throw new IllegalArgumentException("Trick: Player must exist, cannot be null");
        if (playedCard == null) throw new IllegalArgumentException("Trick: PlayedCard must exist, cannot be null");


        if (turns.stream().anyMatch(t -> t.player().equals(player))) {
            throw new IllegalArgumentException("Trick: Player already played in this trick");
        }
        if (!turns.isEmpty()) { // Cards have been played, get leading suit
            Suit leadingSuit = getLeadingSuit();

            if (playedCard.suit() != leadingSuit && player.hasSuit(leadingSuit)) {
                throw new IllegalArgumentException(
                        "Trick: Illegal move: You must follow the leading suit (" + leadingSuit + ").");
            }
        }

        if (turns.size() >= 4) {
            throw new IllegalArgumentException("Trick: Cannot play card" + playedCard + ", this trick already has 4 cards");
        }
//        player.removeCard(playedCard); this is the player's own responsibility
        turns.add(new Turn(player, playedCard));
        if (turns.size() == 4) {
            determineWinner();
        }
    }

    /**
     * Determines the winning player of this trick and sets field: winningPlayer
     */
    private void determineWinner() {
        Suit leadingSuit = getLeadingSuit();

        Player currentWinner = null;
        Card bestCard = null;

        for (Turn turn : turns) {
            Player player = turn.player();
            Card playedCard = turn.playedCard();

            if (bestCard == null) {
                currentWinner = player;
                bestCard = playedCard;
                continue;
            }

            boolean isNewCardTrump = (this.trumpSuit != null && playedCard.suit() == this.trumpSuit);
            boolean isBestCardTrump = (this.trumpSuit != null && bestCard.suit() == this.trumpSuit);

            if (isNewCardTrump) {
                if (!isBestCardTrump) { // If new domain.card.Card is Trump it automatically beats all non-trumps
                    currentWinner = player;
                    bestCard = playedCard;
                } else {                // If both TrumpCards, then highest trump wins
                    if (playedCard.rank().compareTo(bestCard.rank()) > 0) {
                        currentWinner = player;
                        bestCard = playedCard;
                    }
                }
            } else {
                if (!isBestCardTrump) { // If neither are TrumpCards, highest rank wins
                    if (playedCard.suit() == leadingSuit && playedCard.rank().compareTo(bestCard.rank()) > 0) {
                        currentWinner = player;
                        bestCard = playedCard;
                    }
                }
            }
        }
        this.winningPlayer = currentWinner;
    }

    /**
     * Gets the leading suit of this Trick
     *
     * @return the leadingSuit of this Trick (suit of first card played)
     */
    private Suit getLeadingSuit() {
        // get(0) instead of getFirst() for compatibility with earlier version of jdk
        Card firstCard = turns.get(0).playedCard();
        return firstCard.suit();
    }
}
