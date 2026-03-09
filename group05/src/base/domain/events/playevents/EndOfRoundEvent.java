package base.domain.events.playevents;

import base.domain.card.Card;
import base.domain.player.Player;
import base.domain.events.GameEvent;

public record EndOfRoundEvent(Player player , Card card) implements GameEvent {

}
