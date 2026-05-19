package base.storage;

import base.domain.WhistGame;
import base.domain.bid.*;
import base.domain.snapshots.*;
import base.domain.strategy.*;
import base.domain.player.*;

import java.util.List;

/**
 * Application service that converts between live game data and snapshots.
 * Bid history is read from / written to each Round's {@link BidManager};
 * Bid itself is fully player-agnostic.
 *
 * @author John Cai
 * @since 06/04/2026
 */
public class GamePersistenceService {
    private final SaveRepository repository;

    /**
     * Initializes the service with a default repository.
     */
    public GamePersistenceService() {
        this(new SaveRepository());
    }

    /**
     * Initializes the service with a custom repository.
     * @param repository given repository
     * @throws IllegalArgumentException if the repository is null
     */
    public GamePersistenceService(SaveRepository repository) {
        if (repository == null) throw new IllegalArgumentException("Cannot initialize with a null repository");
        this.repository = repository;
    }

    /**
     * Saves the current game state as a snapshot in the repository.
     *
     * @param game the current game instance to be saved
     * @param mode the save mode indicating full game save or count game save
     * @param description a user-chosen description or name for the save, used for choosing between saves when loading
     * @throws IllegalArgumentException if any of the parameters are null or if the description is empty
     * @throws IllegalStateException if the game state is invalid (e.g., missing dealer, malformed rounds)
     */
    public void save(WhistGame game, SaveMode mode, String description) {
        if (game == null) throw new IllegalArgumentException("Cannot save null game");
        if (mode == null) throw new IllegalArgumentException("Cannot save without a save mode ");
        if (description == null) throw new IllegalArgumentException("Cannot save without a description");
        String normalizedDescription = description.trim();
        if (normalizedDescription.isEmpty()) {
            throw new IllegalArgumentException("Save description cannot be empty");
        }

        GameSnapshot rawSnapshot = game.toSnapshot();
        GameSnapshot finalSnapshot = rawSnapshot.withMetaData(normalizedDescription, mode);

        repository.save(finalSnapshot);
    }

    /**
     * Retrieves a list of all save descriptions available in the repository, which can be used to identify and select saves when loading.
     * @return a list of save descriptions
     */
    public List<String> listDescriptions() {
        return repository.listDescriptions();
    }

    /**
     * Loads a saved gameMode from the repository based on the provided description and restores the game state into the given game instance.
     * @param game the game instance to restore the saved state into
     * @param description the name of the saveFile to load
     * @return the SaveMode of the loaded game
     * @throws IllegalArgumentException if given game instance is null, description is null, or no save is found
     * @throws IllegalStateException if the save contains corrupted states that prevent proper restoration
     */
    public SaveMode loadIntoGame(WhistGame game, String description) {
        if (game == null) throw new IllegalArgumentException("Cannot load into a null game");
        if (description == null) throw new IllegalArgumentException("Cannot load from a null description");

        GameSnapshot snapshot = repository.loadByDescription(description);
        game.restoreGame(snapshot);
        return snapshot.mode();
    }
}