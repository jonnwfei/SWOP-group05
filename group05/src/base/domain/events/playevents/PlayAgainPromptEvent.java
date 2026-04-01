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
        List<Integer> scores) implements GameEvent<Integer> {

    @Override
    public Class<Integer> getInputType() {
        return Integer.class;
    }

    @Override
    public boolean isValid(Integer input) {
        return (input == 1 || input == 2);
    }

    @Override
    public boolean needsInput() {
        return true;
    }
}