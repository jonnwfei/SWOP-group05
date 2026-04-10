package base.storage.snapshots;

import java.util.List;

/**
 * @author John Cai
 * @since 03/04/2026
 *
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
    /**
     * Defensive constructor for GameSnapshot
     * @param description description/alias/name for the save, used for choosing between saves when loading
     * @param mode save mode, either full game save or count game save
     * @param dealerIndex index of the dealer for the current round, used for determining turn order and game flow when loading
     * @param players list of player snapshots, containing all player data
     * @throws IllegalArgumentException if description is null
     * @throws IllegalArgumentException if mode is null
     * @throws IllegalArgumentException if dealer index is null
     * @throws IllegalArgumentException if list of playerSnapshots is null
     */
    public GameSnapshot{
        if (description == null) throw new IllegalArgumentException("Description of a playerSnapshot cannot be null");
        if (mode == null) throw new IllegalArgumentException("Mode of a playerSnapshot cannot be null");
        if (players == null) throw new IllegalArgumentException("Players of a playerSnapshot cannot be null");
        if (dealerIndex == null) throw new IllegalArgumentException("Dealer index of a playerSnapshot cannot be null");
        if (dealerIndex < 0) throw new IllegalArgumentException("Dealer index of a playerSnapshot cannot be negative");
        if (dealerIndex > players.size() - 1) throw new IllegalArgumentException("Dealer index of a playerSnapshot cannot exceed player count");

        players = List.copyOf(players);
    }
}
