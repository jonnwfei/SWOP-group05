package base.domain.turn;
import base.domain.card.Card;
import base.domain.player.PlayerId;


/**
 * The type Turn.
 *
 * @author John Cai
 * @since 26/02/2026
 */
public record PlayTurn(PlayerId player, Card playedCard) {
    /**
     * Instantiates a new playTurn.
     *
     * @param player     of this turn
     * @param playedCard of the player in this turn
     */
    public PlayTurn {
        if (player == null) throw new IllegalArgumentException("Play turn: Player cannot be null");
        if (playedCard == null) throw new IllegalArgumentException("Play turn: PlayedCard cannot be null");
    }
}
