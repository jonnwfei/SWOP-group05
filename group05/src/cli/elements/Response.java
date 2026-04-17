package cli.elements;

/**
 * @author Stan Kestens
 * @since 17/04/2026
 *
 * A simple record to encapsulate user input from the terminal.
 * @param rawInput user input, can be null if the event did not require input.
 */
public record Response(String rawInput) {
}