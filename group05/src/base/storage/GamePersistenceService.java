package base.storage;

import base.domain.WhistGame;
import base.domain.deck.Deck;
import base.storage.snapshots.GameSnapshot;
import base.storage.snapshots.PlayerSnapshot;
import base.storage.snapshots.SaveMode;
import base.storage.snapshots.StrategySnapshotType;
import base.domain.player.*;

import java.util.List;

/**
 * Application service that converts between live game data and snapshots.
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
     * Loads a saved gameMode from the repository based on the provided description and restores the game state into the given game instance.
     * @param game the game instance to restore the saved state into
     * @param description the name of the saveFile to load
     * @return the SaveMode of the loaded game, or null if no save with the given description was found
     * @throws IllegalArgumentException if given game instance is null
     * @throws IllegalArgumentException if given description is null
     */
    public SaveMode loadIntoGame(WhistGame game, String description) {
        if (game == null) throw new IllegalArgumentException("Cannot load into a null game");
        if (description == null) throw new IllegalArgumentException("Cannot from a null description");

        GameSnapshot snapshot = repository.loadByDescription(description);
        if (snapshot == null) {
            return null;
        }
        restoreGame(game, snapshot);
        return snapshot.mode();
    }

    /**
     * Constructs a GameSnapshot from the current state of the provided game instance, using the specified save mode and description.
     * @param game the game instance to save
     * @param mode the game mode to save (full game or count session)
     * @param description description/alias or name for the save, used for choosing between saves when loading
     * @return GameSnapshot representing the current state of the game
     */
    private GameSnapshot createSnapshot(WhistGame game, SaveMode mode, String description) {
        String normalizedDescription = description.trim();
        if (normalizedDescription.isEmpty()) {
            throw new IllegalArgumentException("Save description cannot be empty");
        }

        List<Player> players = game.getPlayers();
        List<PlayerSnapshot> snapshots = players.stream().map(this::toSnapshot).toList();

        Player dealer = game.getDealerPlayer();
        if (dealer == null) throw new IllegalStateException("Cannot create snapshot of a game with a null dealer player");
        int dealerIndex = players.indexOf(dealer);
        if (dealerIndex < 0) throw new IllegalStateException("Dealer player must be part of the current players list");

        return new GameSnapshot(normalizedDescription, mode, dealerIndex, snapshots);
    }

    /**
     * Restores the state of the provided game instance based on the data contained in the given GameSnapshot.
     * This includes resetting the game's players and rounds, then re-adding the players with their respective strategies, names, and scores as specified in the snapshot.
     * @param game the game instance to restore the snapshot into
     * @param snapshot the snapshot containing the saved state to restore
     */
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
        }

        game.setDealerPlayer(game.getPlayers().get(snapshot.dealerIndex()));
    }

    /**
     * Constructs a snapshot of a player, containing their name, strategy type, and score from their current state in the game.
     * @param player the player instance to create a snapshot from
     * @return PlayerSnapshot of the provided player
     */
    private PlayerSnapshot toSnapshot(Player player) {
        if (player == null) throw new IllegalArgumentException("Cannot create a snapshot of a null player");
        return new PlayerSnapshot(
                player.getName(),
                toStrategyType(player.getDecisionStrategy()),
                player.getScore());
    }

    /**
     * Converts a player's strategy instance into its corresponding StrategySnapshotType for mapping.
     * @param strategy strategy instance to convert into a snapshot type
     * @return StrategySnapshotType corresponding to the provided strategy instance
     * @throws IllegalArgumentException when trying to convert a null strategy
     */
    private StrategySnapshotType toStrategyType(Strategy strategy) {
        switch (strategy) {
            case null -> throw new IllegalArgumentException("Cannot convert a null strategy");
            case HumanStrategy _ -> {
                return StrategySnapshotType.HUMAN;
            }
            case HighBotStrategy _ -> {
                return StrategySnapshotType.HIGH_BOT;
            }
//            case SmartBotStrategy _ -> { // TODO: fixed by merge
//                return StrategySnapshotType.SMART_BOT;
//            }
            default -> {
                return StrategySnapshotType.LOW_BOT;
            }
        }
    }

    /**
     * Converts a StrategySnapshotType into its corresponding Strategy instance.
     * @param strategyType strategyType instance to convert into a Strategy instance
     * @return Strategy instance
     */
    private Strategy toStrategy(StrategySnapshotType strategyType) {
        return switch (strategyType) {
            case HUMAN -> new HumanStrategy();
            case HIGH_BOT -> new HighBotStrategy();
            case LOW_BOT -> new LowBotStrategy();
//            case SMART_BOT -> new SmartBotStrategy(); // TODO: will be fixed with merge of Strats
        };
    }
}

