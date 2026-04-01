package base.domain.events;

/**
 * A generic contract for all events sent from the Game State to the IO.
 * It dictates what information the IO should render and exactly what
 * type of input it expects in return.
 *
 * @param <T> The expected data type of the user's response
 *            (e.g., Integer, String, or Void for no input).
 */
public interface GameEvent<T> {

    /**
     * Returns the runtime class object of the expected input type.
     * Used by the input parser to determine how to read the terminal.
     *
     * @return The Class literal for type T.
     */
    Class<T> getInputType();

    /**
     * Validates if the parsed input falls within acceptable bounds for this event.
     *
     * @param input The parsed user input of type T.
     * @return true if the input is legally formatted/bounded; false otherwise.
     */
    boolean isValid(T input);

    /**
     * Determines if the Event requires the View to pause and wait for user input.
     *
     * @return true if input is required; false if it is merely a transitional
     *         or informational marker.
     */
    boolean needsInput();
}