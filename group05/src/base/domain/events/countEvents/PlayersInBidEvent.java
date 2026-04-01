package base.domain.events.countEvents;

import base.domain.events.GameEvent;

import java.util.ArrayList;
import java.util.List;

public record PlayersInBidEvent(List<String> playerNames) implements GameEvent<ArrayList<Integer>> {
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
        // Logic: Every winner index must be a valid index in the playerNames list
        for (Integer winnerIndex : input) {
            if (winnerIndex < 1 || winnerIndex > playerNames.size()) {
                return false;
            }
        }
        // Optional: Check for duplicates if one person can't win twice
        long uniqueCount = input.stream().distinct().count();
        return uniqueCount == input.size();
    }

    @Override
    public boolean needsInput() {
        return true;
    }
}