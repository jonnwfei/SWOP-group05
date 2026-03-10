package base.domain.events.playevents;

import base.domain.card.Card;
import base.domain.player.Player;
import base.domain.events.GameEvent;

public record EndOfTrickEvent(String playerName, Card card) implements GameEvent {

}
