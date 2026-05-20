package cli.util;

import cli.TerminalManager;
import cli.events.IOEvent;
import java.util.Arrays;
import java.util.List;

public class TerminalInputHelper {

    private final TerminalManager terminalManager;

    public TerminalInputHelper(TerminalManager terminalManager) {
        this.terminalManager = terminalManager;
    }

    public int askInt(IOEvent event) {
        while (true) {
            try {
                String raw = terminalManager.handle(event).rawInput();
                return Integer.parseInt(raw.trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    public int askInt(IOEvent event, int min, int max) {
        while (true) {
            int value = askInt(event);
            if (value >= min && value <= max) return value;
            System.out.println("Please enter a number between " + min + " and " + max + ".");
        }
    }

    public String askString(IOEvent event) {
        while (true) {
            String raw = terminalManager.handle(event).rawInput();
            if (raw != null && !raw.isBlank()) return raw.trim();
            System.out.println("Input cannot be empty.");
        }
    }

    public List<Integer> askIntList(IOEvent event) {
        while (true) {
            try {
                String raw = terminalManager.handle(event).rawInput();
                if (raw == null || raw.isBlank()) return List.of();
                return Arrays.stream(raw.trim().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Integer::parseInt)
                        .toList();
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter comma-separated numbers.");
            }
        }
    }
}