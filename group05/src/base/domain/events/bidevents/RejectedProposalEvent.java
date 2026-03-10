package base.domain.events.bidevents;

import base.domain.player.Player;
import base.domain.events.GameEvent;

/**
 * A Data Transfer Object (DTO) representing the specific state where a
 * "Proposal" bid was not accepted by any other player.
 * * According to Whist rules, the original proposer must now decide
 * whether to play "Solo" or change their bid to "Pass".
 *
 * @param proposerName The name of the player who made the original Proposal
 * and is now being prompted for their final decision.
 */
public record RejectedProposalEvent(String proposerName) implements GameEvent {
}
