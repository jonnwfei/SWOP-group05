package base.domain.events.playevents;

import base.domain.events.GameEvent;

import java.util.List;

/**
 * A Data Transfer Object (DTO) used at the end of a round or game.
 * It provides the View with the final scores and signals it to ask the user
 * whether they want to play again or quit.
 */
public record PlayAgainPromptEvent(
        List<String> playerNames,
        List<Integer> scores
) implements GameEvent {
    public PlayAgainPromptEvent {
        playerNames = List.copyOf(playerNames);
        scores = List.copyOf(scores);
    }
}