package base.domain.trick;
import base.domain.card.Card;
import base.domain.player.Player;


/**
 * The type Turn.
 *
 * @author John Cai
 * @since 26/02/2026
 */
public record Turn(Player player, Card playedCard) {
    /**
     * Instantiates a new Turn.
     *
     * @param player     of this turn
     * @param playedCard of the player in this turn
     */
    public Turn {
        if (player == null) throw new IllegalArgumentException("Turn: Player cannot be null");
        if (playedCard == null) throw new IllegalArgumentException("Turn: PlayedCard cannot be null");
    }

    /**
     * @return Stringified Turn, e.g. "Turn"
     */
    @Override
    public String toString() {
        return player.getName() + " played " + playedCard;
    }
}
