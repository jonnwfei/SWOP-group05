package base.domain.trick;

import base.domain.player.Player;
import base.domain.card.Card;

/**
 * @author John Cai
 * @since 26/02/2026
 */
public class Turn {
    private final Player player;
    private final Card playedCard;

    /**
     * @param player of this turn
     * @param playedCard of the player in this turn
     */
    public Turn(Player player, Card playedCard) {
        this.player = player;
        this.playedCard = playedCard;
    }

    /**
     * @return played card in this turn by the player
     */
    public Card getPlayedCard() {
        return playedCard;
    }

    /**
     * @return player of this turn
     */
    public Player getPlayer() {
        return player;
    }
}
