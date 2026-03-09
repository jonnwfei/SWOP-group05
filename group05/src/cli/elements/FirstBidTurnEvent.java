package cli.elements;

import base.domain.card.Suit;
import base.domain.player.Player;

public record FirstBidTurnEvent(Player currentPlayer, Suit dealtTrump) implements GameEvent{
}
