package base.storage;

import base.domain.WhistGame;
import base.domain.bid.*;
import base.domain.deck.Deck;
import base.domain.round.Round;
import base.domain.snapshots.*;
import base.domain.strategy.*;
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
     * @throws IllegalStateException if the game state is invalid (e.g., missing dealer, malformed rounds)
     */
    public void save(WhistGame game, SaveMode mode, String description) {
        if (game == null) throw new IllegalArgumentException("Cannot save null game");
        if (mode == null) throw new IllegalArgumentException("Cannot save without a save mode ");
        if (description == null) throw new IllegalArgumentException("Cannot save without a description");

        GameSnapshot snapshot = game.toSnapshot(mode, description);
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
     * @return the SaveMode of the loaded game
     * @throws IllegalArgumentException if given game instance is null, description is null, or no save is found
     * @throws IllegalStateException if the save contains corrupted states that prevent proper restoration
     */
    public SaveMode loadIntoGame(WhistGame game, String description) {
        if (game == null) throw new IllegalArgumentException("Cannot load into a null game");
        if (description == null) throw new IllegalArgumentException("Cannot load from a null description");

        GameSnapshot snapshot = repository.loadByDescription(description);
        restoreGame(game, snapshot);
        return snapshot.mode();
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
            PlayerId restoredId = PlayerId.fromString(playerSnapshot.id());
            Strategy playerStrategy = toStrategy(playerSnapshot.strategyType(), restoredId);

            Player player = new Player(playerStrategy, playerSnapshot.name(), restoredId);
            player.updateScore(playerSnapshot.score());
            game.addPlayer(player);
        }

        restoreRoundHistory(game, snapshot.rounds());

        if (snapshot.mode() == SaveMode.GAME) {
            game.setDeck(new Deck());
        }

        game.setDealerPlayer(game.getAllPlayers().get(snapshot.dealerIndex()));
    }

    // =================================================================================
    // Extraction Helpers
    // =================================================================================


    /**
     * Rebuilds round history placeholders so round-based workflows keep functioning after load.
     * @param game restored game
     * @param roundSnapshots persisted round snapshots
     * @throws IllegalStateException if trying to restore rounds to a game without exactly 4 players
     */
    private void restoreRoundHistory(WhistGame game, List<RoundSnapshot> roundSnapshots) {
            if (roundSnapshots.isEmpty()) {
                return;
            }

            List<Player> players = game.getPlayers();
            if (players.size() != 4) throw new IllegalStateException("Cannot restore rounds without exactly 4 players");

            for (RoundSnapshot snapshot : roundSnapshots) {
                // No bounds checking needed! RoundSnapshot guarantees indices are 0-3.
                Player mainBidder = players.get(snapshot.bidderIndex());
                Bid highestBid = snapshot.bidType().instantiate(mainBidder.getId(), snapshot.trumpSuit());

                // Beautiful, clean mapping
                List<Player> participants = snapshot.participantIndices().stream()
                        .map(players::get)
                        .toList();

                List<Player> miserieWinners = snapshot.miserieWinnerIndices().stream()
                        .map(players::get)
                        .toList();

                Round restoredRound = new Round(players, mainBidder, snapshot.multiplier());
                restoredRound.restoreFromSnapshot(
                        highestBid,
                        snapshot.trumpSuit(),
                        participants,
                        snapshot.tricksWon(),
                        miserieWinners,
                        snapshot.scoreDeltas());

                game.addRound(restoredRound);
            }
        }

    /**
     * Converts a StrategySnapshotType into its corresponding Strategy instance.
     * @param strategyType strategyType instance to convert into a Strategy instance
     * @return Strategy instance
     */
    private Strategy toStrategy(StrategySnapshotType strategyType, PlayerId restoredId) {
        return switch (strategyType) {
            case HUMAN -> new HumanStrategy();
            case HIGH_BOT -> new HighBotStrategy();
            case LOW_BOT -> new LowBotStrategy();
            case SMART_BOT -> new SmartBotStrategy(restoredId);
        };
    }
}

