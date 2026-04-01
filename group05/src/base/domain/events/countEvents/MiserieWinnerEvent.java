package base.domain.events.countEvents;

import base.domain.events.GameEvent;

import java.util.ArrayList;
import java.util.List;

public record MiserieWinnerEvent(List<String> playerNames) implements GameEvent<ArrayList<Integer>> {
    @Override
    public Class<ArrayList<Integer>> getInputType() {
        // We cast to the raw class because ArrayList<Integer>.class doesn't exist
        return (Class<ArrayList<Integer>>) (Class<?>) ArrayList.class;
    }

    @Override
    public boolean isValid(ArrayList<Integer> input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        // Special Case: If the list is exactly [-1], it is valid (e.g., "No Winners")
        if (input.size() == 1 && input.getFirst() == -1) {
            return true;
        }

        // Standard Case: Check every index against the player list
        for (Integer winnerIndex : input) {
            if (winnerIndex < 1 || winnerIndex > playerNames.size()) {
                return false;
            }
        }

        // Ensure no duplicate player indices (e.g., [1, 1, 2] is invalid)
        long uniqueCount = input.stream().distinct().count();
        return uniqueCount == input.size();
    }

    @Override
    public boolean needsInput() {
        return true;
    }
}
