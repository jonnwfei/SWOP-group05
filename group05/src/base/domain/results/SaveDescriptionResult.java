package base.domain.results;

import base.storage.snapshots.SaveMode;

/**
 * Signals that a save-description prompt is expected and carries the persistence intent.
 * <p>
 * The state never touches IO; the adapter reads {@link #mode()} to perform the save.
 */
public record SaveDescriptionResult(SaveMode mode) implements GameResult {
    public SaveDescriptionResult {
        if (mode == null) throw new IllegalArgumentException("SaveMode cannot be null");
    }
}