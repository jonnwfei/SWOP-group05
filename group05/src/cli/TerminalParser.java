package cli;

import java.util.ArrayList;

public class TerminalParser {

    /**
     * Converts string to a single integer.
     * Throws IllegalArgumentException if not a number.
     */
    public int parseNumberInput(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Input cannot be null or empty.");
        }
        try {
            return Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + input + "' is not a valid number.");
        }
    }

    /**
     * Converts a comma-separated string into a list of integers.
     */
    public ArrayList<Integer> parseNumbersInput(String input) {
        // We only check for null to avoid a crash;
        // blank strings just result in an empty list.
        if (input == null || input.isBlank()) {
            return new ArrayList<>();
        }

        String[] parts = input.split(",");
        ArrayList<Integer> result = new ArrayList<>();

        for (String part : parts) {
            String trimmed = part.trim();

            // Skip empty segments like "1,,3" instead of throwing an error
            if (trimmed.isEmpty()) continue;

            try {
                result.add(Integer.parseInt(trimmed));
            } catch (NumberFormatException e) {
                // We still throw this because we can't turn "abc" into an Integer
                throw new IllegalArgumentException("'" + trimmed + "' is not a valid number.");
            }
        }
        return result;
    }

    /**
     * Simply returns the string, but ensures it's not null/empty.
     */
    public String parseString(String input) {
        if (input == null ) {
            throw new IllegalArgumentException("Input cannot be null.");
        }
        return input.trim();
    }
}