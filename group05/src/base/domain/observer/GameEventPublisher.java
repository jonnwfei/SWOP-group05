package base.domain.observer;

/**
 * Exposes only the ability to subscribe to game events.
 * Prevents clients from mutating the underlying game state.
 */
public interface GameEventPublisher {
    void addObserver(GameObserver observer);
}