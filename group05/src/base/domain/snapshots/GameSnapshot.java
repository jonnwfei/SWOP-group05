package base.domain.snapshots;

import java.util.List;

/**
 * Serializable snapshot of all data required to continue a session.
 * containing:
 * <ul>
 *      <li>description of the save, name for example</li>
 *      <li>which mode (Count vs Game)</li>
 *      <li>Dealer index</li>
 *      <li>List of playerSnapshots</li>
 * </ul>

 */
public record GameSnapshot(String description, SaveMode mode, Integer dealerIndex, List<PlayerSnapshot> players) {
}
