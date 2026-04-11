package base.domain.snapshots;

/**
 * @author John Cai
 * @since 03/04/2026
 *
 * Snapshot of one player for persistence.
 * Containing:
 * <ul>
 *     <li>Player name</li>
 *     <li>StrategyType, bot or player</li>
 *     <li>Score</li>
 * </ul>
 */
public record PlayerSnapshot(String name, StrategySnapshotType strategyType, int score) {
    /**
     * Defensive constructor for playerSnapshot
     * @param name name of the player
     * @param strategyType strategyType of the player, either bot or human
     * @param score score of the player
     * @throws IllegalArgumentException if name of player is null or blank
     * @throws IllegalArgumentException if strategyType of player is null
     */
    public PlayerSnapshot {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name of a playerSnapshot cannot be null or blank");
        if (strategyType == null) throw new IllegalArgumentException("StrategyType of a playerSnapshot cannot be null");
    }
}
