package base.domain.events;

public interface GameEvent<T> {
    /**
     * Returns the Class type (e.g., Integer.class, String.class).
     */
    Class<T> getInputType();

    /**
     * Returns true if the input meets the rules, false otherwise.
     */
    boolean isValid(T input);
    boolean needsInput();
}