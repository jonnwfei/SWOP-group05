package base.domain.events.countEvents;

import base.domain.events.GameEvent;

import java.util.List;

/**
 * A Data Transfer Object (DTO) used during the manual Count phase to identify
 * which players participated in the specific bid being recorded.
 * * * This event is dispatched after the Bid and Trump have been selected.
 * The View uses this list of names to allow the user to select the "Bidding Team"
 * (e.g., in a Proposal/Acceptance, the user would select both the proposer
 * and the accepter).
 *
 * @param playerNames An immutable list of all player names currently in the
 * game session, used to build a selection menu (e.g., "[1] Alice, [2] Bob...").
 */
public record PlayersInBidEvent(List<String> playerNames) implements GameEvent {
    public PlayersInBidEvent {
        playerNames = List.copyOf(playerNames); // Defensive copy
    }
}