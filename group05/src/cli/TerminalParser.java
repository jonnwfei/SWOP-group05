package cli;

import java.util.ArrayList;

/**
 * Utility for parsing and sanitizing terminal input into structured data.
 * @author Stan Kestens
 * @since 02/03/2026
 */
public class TerminalParser {

    /**
     * Parses a string into an integer.
     * @param input Raw string from scanner.
     * @return Parsed integer.
     * @throws IllegalArgumentException if blank or non-numeric.
     */
    public int parseNumberInput(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Input cannot be empty.");
        }
        try {
            return Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + input + "' is not a number.");
        }
    }

    /**
     * Parses a comma-separated string into a list of integers.
     * @param input String like "1, 2, 3".
     * @return List of parsed integers. (like [1,2,3])
     */
    public ArrayList<Integer> parseNumbersInput(String input) {
        if (input == null || input.isBlank()) return new ArrayList<>();

        String[] parts = input.split(",");
        ArrayList<Integer> result = new ArrayList<>();

        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;
            try {
                result.add(Integer.parseInt(trimmed));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("'" + trimmed + "' is not a number.");
            }
        }
        return result;
    }

    /**
     * Trims whitespace and ensures non-null.
     * @param input Raw string.
     * @return Cleaned string.
     */
    public String parseString(String input) {
        if (input == null) throw new IllegalArgumentException("Input cannot be null.");
        return input.trim();
    }
}