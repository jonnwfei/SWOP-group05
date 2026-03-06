package base.domain.trick;
import base.domain.card.Card;
import base.domain.player.Player;


/**
 * The type Turn.
 *
 * @author John Cai
 * @since 26/02/2026
 */
public class Turn {
    private final Player player;
    private final Card playedCard;

    /**
     * Instantiates a new Turn.
     *
     * @param player     of this turn
     * @param playedCard of the player in this turn
     */
    public Turn(Player player, Card playedCard) {
        this.player = player;
        this.playedCard = playedCard;
    }

    /**
     * Gets played card.
     *
     * @return played card in this turn by the player
     */
    public Card getPlayedCard() {
        return playedCard;
    }

    /**
     * Gets player.
     *
     * @return player of this turn
     */
    public Player getPlayer() {
        return player;
    }
}
