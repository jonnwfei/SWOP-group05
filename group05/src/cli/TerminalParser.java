package cli;

import java.util.ArrayList;

public class TerminalParser {

    public int parseNumberInput(String input, int lowerBound, int upperBound) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Input cannot be null or empty.");
        }
        int parsedValue;
        try {
            parsedValue = Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Input must be a valid integer: " + input);
        }
        // Range Validation
        if (parsedValue < lowerBound || parsedValue > upperBound) {
            throw new IllegalArgumentException(
                    String.format("Input %d is out of bounds (Min: %d, Max: %d)",
                            parsedValue, lowerBound, upperBound)
            );
        }
        return parsedValue;
    }
    public ArrayList<Integer> parseNumbersInput(String input, int lowerBound, int upperBound) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Input cannot be null or empty");
        }

        // Split by comma and create the result list
        String[] parts = input.split(",");
        ArrayList<Integer> result = new ArrayList<>();

        for (String part : parts) {
            String trimmedPart = part.trim();
            // Skip empty entries (e.g., if input was "1,,4")
            if (trimmedPart.isEmpty()) {
                throw new IllegalArgumentException("Input contains an empty value between commas");
            }
            try {
                int parsedValue = Integer.parseInt(trimmedPart);
                // Bounds Validation
                if (parsedValue < lowerBound || parsedValue > upperBound) {
                    throw new IllegalArgumentException(
                            String.format("Value %d is out of bounds [%d, %d]",
                                    parsedValue, lowerBound, upperBound)
                    );
                }
                result.add(parsedValue);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid integer found: " + trimmedPart);
            }
        }
        return result;
    }
    public String ParseString(String string){
        return string;
    }



}
