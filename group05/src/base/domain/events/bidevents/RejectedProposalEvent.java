package base.domain.events.bidevents;

import base.domain.player.Player;
import base.domain.events.GameEvent;

public record RejectedProposalEvent(Player proposer) implements GameEvent {
}
