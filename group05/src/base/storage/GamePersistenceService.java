package base.storage;

import base.domain.WhistGame;
import base.domain.deck.Deck;
import base.domain.snapshots.GameSnapshot;
import base.domain.snapshots.PlayerSnapshot;
import base.domain.snapshots.SaveMode;
import base.domain.snapshots.StrategySnapshotType;
import base.domain.player.*;

import java.util.List;

/**
 * Application service that converts between live game data and snapshots.
 */
public class GamePersistenceService {
    private final SaveRepository repository;

    /**
     * Initializes the service with a default repository implementation.
     */
    public GamePersistenceService() {
        this(new SaveRepository());
    }

    /**
     * Initializes the service with a custom repository implementation.
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
     */
    public void save(WhistGame game, SaveMode mode, String description) {
        if (game == null) throw new IllegalArgumentException("Cannot save null game");
        if (mode == null) throw new IllegalArgumentException("Cannot save without a save mode ");
        if (description == null) throw new IllegalArgumentException("Cannot save without a description");

        GameSnapshot snapshot = createSnapshot(game, mode, description);
        repository.save(snapshot);
    }

    /**
     * Retrieves a list of all save descriptions available in the repository, which can be used to identify and select saves when loading.
     * @return a list of save descriptions
     */
    public List<String> listDescriptions() {
        return repository.listDescriptions();
    }

    /**
     * Loads a saved gameMode from the repository based on the provided description.
     * @param game the game instance to restore the saved state into
     * @param description the name of the saveFile to load
     * @return the SaveMode of the loaded game, or null if no save with the given description was found
     */
    public SaveMode loadIntoGame(WhistGame game, String description) {
        GameSnapshot snapshot = repository.loadByDescription(description);
        if (snapshot == null) {
            return null;
        }
        restoreGame(game, snapshot);
        return snapshot.mode();
    }

    /**
     * Constructs a GameSnapshot from the current state of the provided game instance, using the specified save mode and description.
     * @param game
     * @param mode
     * @param description
     * @return
     */
    private GameSnapshot createSnapshot(WhistGame game, SaveMode mode, String description) {
        String normalizedDescription = description == null ? "" : description.trim();
        if (normalizedDescription.isEmpty()) {
            throw new IllegalArgumentException("Save description cannot be empty");
        }

        List<Player> players = game.getPlayers();
        List<PlayerSnapshot> snapshots = players.stream().map(this::toSnapshot).toList();

        Integer dealerIndex = null;
        Player dealer = game.getDealerPlayer();
        if (dealer != null) {
            int index = players.indexOf(dealer);
            dealerIndex = index >= 0 ? index : null;
        }

        return new GameSnapshot(normalizedDescription, mode, dealerIndex, snapshots);
    }

    private void restoreGame(WhistGame game, GameSnapshot snapshot) {
        game.resetPlayers();
        game.resetRounds();

        for (PlayerSnapshot playerSnapshot : snapshot.players()) {
            Player player = new Player(toStrategy(playerSnapshot.strategyType()), playerSnapshot.name());
            player.updateScore(playerSnapshot.score());
            game.addPlayer(player);
        }

        if (snapshot.mode() == SaveMode.GAME) {
            game.setDeck(new Deck());
            if (snapshot.dealerIndex() != null && snapshot.dealerIndex() >= 0 && snapshot.dealerIndex() < game.getPlayers().size()) {
                game.setDealerPlayer(game.getPlayers().get(snapshot.dealerIndex()));
            } else {
                game.setRandomDealer();
            }
        } else {
            game.setDealerPlayer(null);
        }
    }

    private PlayerSnapshot toSnapshot(Player player) {
        return new PlayerSnapshot(
                player.getName(),
                toStrategyType(player.getDecisionStrategy()),
                player.getScore());
    }

    private StrategySnapshotType toStrategyType(Strategy strategy) {
        if (strategy instanceof HumanStrategy) {
            return StrategySnapshotType.HUMAN;
        }
        if (strategy instanceof HighBotStrategy) {
            return StrategySnapshotType.HIGH_BOT;
        }
        return StrategySnapshotType.LOW_BOT;
    }

    private Strategy toStrategy(StrategySnapshotType strategyType) {
        return switch (strategyType) {
            case HUMAN -> new HumanStrategy();
            case HIGH_BOT -> new HighBotStrategy();
            case LOW_BOT -> new LowBotStrategy();
        };
    }
}

