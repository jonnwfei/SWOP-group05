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
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Input cannot be null or empty.");
        }

        String[] parts = input.split(",");
        ArrayList<Integer> result = new ArrayList<>();

        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException("Found an empty value between commas.");
            }
            try {
                result.add(Integer.parseInt(trimmed));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("'" + trimmed + "' is not a valid number.");
            }
        }
        return result;
    }

    /**
     * Simply returns the string, but ensures it's not null/empty.
     */
    public String parseString(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Input cannot be null or empty.");
        }
        return input.trim();
    }
}