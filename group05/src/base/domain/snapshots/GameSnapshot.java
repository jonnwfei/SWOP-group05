package base.domain.snapshots;

import base.domain.WhistRules;

import java.util.List;

/**
 * Serializable snapshot of all data required to continue a session.
 * <p>
 * Containing:
 * <ul>
 * <li>Description of the save (e.g., user-chosen name)</li>
 * <li>The save mode (Count vs Game)</li>
 * <li>Dealer index</li>
 * <li>List of player snapshots</li>
 * <li>List of round snapshots</li>
 * </ul>
 * @author John Cai
 * @since 03/04/2026
 */
public record GameSnapshot(String description, SaveMode mode, Integer dealerIndex, List<PlayerSnapshot> players,
        List<RoundSnapshot> rounds) {
    /**
     * Defensive constructor for GameSnapshot
     * 
     * @param description description/alias/name for the save, used for choosing
     *                    between saves when loading
     * @param mode        save mode, either full game save or count game save
     * @param dealerIndex index of the dealer for the current round, used for
     *                    determining turn order and game flow when loading
     * @param players     list of player snapshots, containing all player data
     * @throws IllegalArgumentException if description is null
     * @throws IllegalArgumentException if mode is null
     * @throws IllegalArgumentException if dealer index is null
     * @throws IllegalArgumentException if list of gameSnapshots is null
     */
    public GameSnapshot {
        if (description == null)
            throw new IllegalArgumentException("Description of a gameSnapshot cannot be null");
        if (mode == null)
            throw new IllegalArgumentException("Mode of a gameSnapshot cannot be null");

        if (players == null || players.isEmpty())
            throw new IllegalArgumentException("PlayerSnapshots of a gameSnapshot cannot be null or empty");
        if (players.size() < 4)
            throw new IllegalArgumentException("A saved Whist game must contain at least 4 players");
        for (PlayerSnapshot player : players) {
            if (player == null)
                throw new IllegalArgumentException("A PlayerSnapshot inside the list cannot be null");
        }

        if (rounds == null)
            throw new IllegalArgumentException("RoundSnapshots of a gameSnapshot cannot be null");
        for (RoundSnapshot round : rounds) {
            if (round == null)
                throw new IllegalArgumentException("A RoundSnapshot inside the list cannot be null");

        }
        if (dealerIndex == null)
            throw new IllegalArgumentException("Dealer index of a gameSnapshot cannot be null");
        if (dealerIndex < 0)
            throw new IllegalArgumentException("Dealer index of a gameSnapshot cannot be negative");
        if (dealerIndex > 3)
            throw new IllegalArgumentException("Dealer index of a playerSnapshot cannot exceed player count");

        players = List.copyOf(players);
        rounds = List.copyOf(rounds);
    }
}
