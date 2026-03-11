package base.domain.events.errorEvents;

import base.domain.events.GameEvent;

import java.util.ArrayList;

public record NumberListErrorEvent(int lowerBound, int upperBound) implements GameEvent<ArrayList<Integer>> {
    @Override
    public Class<ArrayList<Integer>> getInputType() {
        return (Class<ArrayList<Integer>>) (Class<?>) ArrayList.class;
    }

    @Override
    public boolean isValid(ArrayList<Integer> input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        // Logic: Every winner index must be a valid index in the playerNames list
        for (Integer number : input) {
            if (number < lowerBound || number>upperBound) {
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
