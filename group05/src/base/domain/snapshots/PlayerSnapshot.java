package base.domain.snapshots;

/**
 * Snapshot of one player for persistence.
 * Containing:
 * <ul>
 *     <li>Player name</li>
 *     <li>StrategyType, bot or player</li>
 *     <li>Score</li>
 * </ul>
 */
public record PlayerSnapshot(String name, StrategySnapshotType strategyType, int score) {
}
