package base.storage.snapshots;

/**
 * Distinguishes which flow should be resumed after loading a save.
 * <br>
 *  - GAME: Resumes an active game, returning to the game screen.
 * <br>
 *  - COUNT: Resumes the score counting flow, returning to the score counting screen.
 * <br>
 */
public enum SaveMode {
    GAME,
    COUNT
}

