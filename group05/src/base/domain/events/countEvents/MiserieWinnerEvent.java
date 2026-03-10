package base.domain.events.countEvents;

import base.domain.events.GameEvent;
import base.domain.player.Player;

import java.util.List;

/**
 * A Data Transfer Object (DTO) used during the manual Count phase specifically
 * for "Miserie" or "Open Miserie" bids.
 * * In these special bids, multiple players may be "winners" if they successfully
 * avoided winning any tricks. This event prompts the UI to provide a multi-selection
 * interface for identifying these players.
 *
 * @param playerNames An immutable list of all player names currently in the game,
 * used by the View to build a selection menu (e.g., "1: Tom, 2: Sarah...").
 */
public record MiserieWinnerEvent(List<String> playerNames) implements GameEvent {
    public MiserieWinnerEvent {
        playerNames = List.copyOf(playerNames); // Defensive copy
    }
}
