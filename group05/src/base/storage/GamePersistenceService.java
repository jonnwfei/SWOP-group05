package base.storage;

import base.domain.WhistGame;
import base.domain.bid.*;
import base.domain.deck.Deck;
import base.domain.round.Round;
import base.domain.strategy.*;
import base.storage.snapshots.*;
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
     * Constructs a GameSnapshot from the current state of the provided game instance, using the specified save mode and description.
     * @param game the game instance to save
     * @param mode the game mode to save (full game or count session)
     * @param description description/alias or name for the save, used for choosing between saves when loading
     * @return GameSnapshot representing the current state of the game
     * @throws IllegalArgumentException if the description is blank
     * @throws IllegalStateException if the dealer is null or not in the players list
     */
    private GameSnapshot createSnapshot(WhistGame game, SaveMode mode, String description) {
        String normalizedDescription = description.trim();
        if (normalizedDescription.isEmpty()) {
            throw new IllegalArgumentException("Save description cannot be empty");
        }

        List<Player> players = game.getPlayers();
        List<PlayerSnapshot> snapshots = players.stream().map(this::toSnapshot).toList();

        List<Round> rounds = game.getRounds();
        List<RoundSnapshot> roundSnapshots = rounds.stream().map(this::toSnapshot).toList();

        Player dealer = game.getDealerPlayer();
        if (dealer == null) throw new IllegalStateException("Cannot create snapshot of a game with a null dealer player");
        int dealerIndex = players.indexOf(dealer);
        if (dealerIndex < 0) throw new IllegalStateException("Dealer player must be part of the current players list");

        return new GameSnapshot(normalizedDescription, mode, dealerIndex, snapshots, roundSnapshots);
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

        game.setDealerPlayer(game.getPlayers().get(snapshot.dealerIndex()));
    }

    /**
     * Constructs a snapshot of a player, containing their name, strategy type, and score from their current state in the game.
     * @param player the player instance to create a snapshot from
     * @return PlayerSnapshot of the provided player
     * @throws IllegalArgumentException if the player is null
     */
    private PlayerSnapshot toSnapshot(Player player) {
        if (player == null) throw new IllegalArgumentException("Cannot create a snapshot of a null player");
        return new PlayerSnapshot(
                player.getId().id().toString(),
                player.getName(),
                toStrategyType(player.getDecisionStrategy()),
                player.getScore());
    }

    /**
     * Constructs a snapshot of a round for persistence.
     * This currently captures stable metadata and round count compatibility fields.
     * @param round the round instance to create a snapshot from
     * @return RoundSnapshot of the provided round
     * @throws IllegalArgumentException if the round is null
     * @throws IllegalStateException if the round's internal state is corrupted or missing essential data
     */
    private RoundSnapshot toSnapshot(Round round) {
        if (round == null) throw new IllegalArgumentException("Cannot create a snapshot of a null round");

        Bid highestBid = round.getHighestBid();
        if (highestBid == null) throw new IllegalStateException("Cannot snapshot a round without a highest bid");

        List<Player> roundPlayers = round.getPlayers();
        if (roundPlayers.size() != 4) throw new IllegalStateException("Cannot snapshot round without exactly 4 players");

        BidType bidType = highestBid.getType();
        int bidderIndex = roundPlayers.indexOf(round.getPlayerById(highestBid.getPlayerId()));
        if (bidderIndex < 0) {
            throw new IllegalStateException("Cannot snapshot round: highest bid player is not in round players");
        }


        List<Integer> participantIndices = round.getBiddingTeamPlayers().stream()
            .map(roundPlayers::indexOf)
            .toList();
        if (participantIndices.stream().anyMatch(i -> i < 0)) {
            throw new IllegalStateException("Cannot snapshot round: bidding team contains players outside the round");
        }
        if (participantIndices.isEmpty() && bidType != BidType.PASS) {
            throw new IllegalStateException("Cannot snapshot round without bidding team participants");
        }

        List<Integer> miserieWinnerIndices = round.getCountMiserieWinners().stream()
            .map(roundPlayers::indexOf)
            .toList();
        if (miserieWinnerIndices.stream().anyMatch(i -> i < 0)) {
            throw new IllegalStateException("Cannot snapshot round: miserie winners contain players outside the round");
        }

        List<Integer> scoreDeltas = round.getScoreDeltas();

        int tricksWon = round.getCountTricksWon();
        if (tricksWon < 0) {
            tricksWon = round.getBiddingTeamTricksWon();
        }

        try {
            return new RoundSnapshot(
                    bidType,
                    bidderIndex,
                    participantIndices,
                    tricksWon,
                    miserieWinnerIndices,
                    round.getMultiplier(),
                    scoreDeltas,
                    round.getTrumpSuit());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Round contains invalid data: " + e.getMessage());
        }
    }

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
            int bidderIndex = snapshot.bidderIndex();
            if (bidderIndex < 0 || bidderIndex >= players.size()) {
                throw new IllegalStateException("Cannot restore round: bidder index out of range");
            }

            Player mainBidder = players.get(bidderIndex);
            Bid highestBid = snapshot.bidType().instantiate(mainBidder.getId(), snapshot.trumpSuit());

            List<Player> participants = snapshot.participantIndices().stream()
                    .map(index -> {
                        if (index < 0 || index >= players.size()) {
                            throw new IllegalStateException("Cannot restore round: participant index out of range");
                        }
                        return players.get(index);
                    })
                    .toList();

            List<Player> miserieWinners = snapshot.miserieWinnerIndices().stream()
                    .map(index -> {
                        if (index < 0 || index >= players.size()) {
                            throw new IllegalStateException("Cannot restore round: miserie winner index out of range");
                        }
                        return players.get(index);
                    })
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
            case LowBotStrategy _ -> {
                return StrategySnapshotType.LOW_BOT;
            }
            case SmartBotStrategy _  -> {
                return StrategySnapshotType.SMART_BOT;
            }
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

