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
 * Bid history is read from / written to each Round's {@link BidManager};
 * Bid itself is fully player-agnostic.
 *
 * @author John Cai
 * @since 06/04/2026
 */
public class GamePersistenceService {
    private final SaveRepository repository;

    public GamePersistenceService() { this(new SaveRepository()); }

    public GamePersistenceService(SaveRepository repository) {
        if (repository == null) throw new IllegalArgumentException("Cannot initialize with a null repository");
        this.repository = repository;
    }

    public void save(WhistGame game, SaveMode mode, String description) {
        if (game == null) throw new IllegalArgumentException("Cannot save null game");
        if (mode == null) throw new IllegalArgumentException("Cannot save without a save mode ");
        if (description == null) throw new IllegalArgumentException("Cannot save without a description");

        repository.save(createSnapshot(game, mode, description));
    }

    public List<String> listDescriptions() { return repository.listDescriptions(); }

    public SaveMode loadIntoGame(WhistGame game, String description) {
        if (game == null) throw new IllegalArgumentException("Cannot load into a null game");
        if (description == null) throw new IllegalArgumentException("Cannot load from a null description");

        GameSnapshot snapshot = repository.loadByDescription(description);
        restoreGame(game, snapshot);
        return snapshot.mode();
    }

    // =================================================================================
    // Snapshot creation
    // =================================================================================

    private GameSnapshot createSnapshot(WhistGame game, SaveMode mode, String description) {
        String normalizedDescription = description.trim();
        if (normalizedDescription.isEmpty())
            throw new IllegalArgumentException("Save description cannot be empty");

        List<Player> allPlayers = game.getAllPlayers();
        List<PlayerSnapshot> snapshots = allPlayers.stream().map(this::toSnapshot).toList();

        List<Round> rounds = game.getRounds();
        List<RoundSnapshot> roundSnapshots = rounds.stream().map(this::toSnapshot).toList();

        Player dealer = game.getDealerPlayer();
        if (dealer == null) throw new IllegalStateException("Cannot create snapshot of a game with a null dealer player");
        int dealerIndex = allPlayers.indexOf(dealer);
        if (dealerIndex < 0) throw new IllegalStateException("Dealer player must be part of the current players list");

        return new GameSnapshot(normalizedDescription, mode, dealerIndex, snapshots, roundSnapshots);
    }

    private PlayerSnapshot toSnapshot(Player player) {
        if (player == null) throw new IllegalArgumentException("Cannot create a snapshot of a null player");
        return new PlayerSnapshot(
                player.getId().id().toString(),
                player.getName(),
                toStrategyType(player.getDecisionStrategy()),
                player.getScore());
    }

    private RoundSnapshot toSnapshot(Round round) {
        if (round == null) throw new IllegalArgumentException("Cannot create a snapshot of a null round");

        Bid highestBid = round.getHighestBid();
        if (highestBid == null) throw new IllegalStateException("Cannot snapshot a round without a highest bid");

        List<Player> roundPlayers = round.getPlayers();
        if (roundPlayers.size() != 4) throw new IllegalStateException("Cannot snapshot round without exactly 4 players");

        BidType bidType = highestBid.getType();

        int bidderIndex = resolveBidderIndex(round, highestBid, roundPlayers);
        List<Integer> participantIndices = resolveParticipantIndices(round, roundPlayers, bidType);
        List<Integer> miserieWinnerIndices = resolveMiserieWinnerIndices(round, roundPlayers, bidType);
        int tricksWon = resolveTricksWon(round, bidType);

        List<Integer> scoreDeltas = round.getScoreDeltas();
        if (scoreDeltas == null || scoreDeltas.size() != 4)
            throw new IllegalStateException("Cannot snapshot round: score deltas must contain exactly 4 values");

        try {
            return new RoundSnapshot(
                    bidType, bidderIndex, participantIndices, tricksWon,
                    miserieWinnerIndices, round.getMultiplier(), scoreDeltas, round.getTrumpSuit());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Round contains invalid data: " + e.getMessage());
        }
    }

    // =================================================================================
    // Extraction helpers
    // =================================================================================

    /**
     * Asks the per-round BidManager which player placed the winning bid.
     * Replaces the previous {@code highestBid.getPlayerId()} call now that Bid
     * is player-agnostic.
     */
    private int resolveBidderIndex(Round round, Bid highestBid, List<Player> roundPlayers) {
        PlayerId bidderId = round.getBidManager().getBidderOf(highestBid);
        int bidderIndex = roundPlayers.indexOf(round.getPlayerById(bidderId));
        if (bidderIndex < 0)
            throw new IllegalStateException("Cannot snapshot round: highest bid player is not in round players");
        return bidderIndex;
    }

    private List<Integer> resolveParticipantIndices(Round round, List<Player> roundPlayers, BidType bidType) {
        List<Integer> participantIndices = round.getBiddingTeamPlayers().stream()
                .map(roundPlayers::indexOf).toList();

        if (participantIndices.stream().anyMatch(i -> i < 0))
            throw new IllegalStateException("Cannot snapshot round: bidding team contains players outside the round");
        if (participantIndices.isEmpty() && bidType != BidType.PASS)
            throw new IllegalStateException("Cannot snapshot round without bidding team participants");
        return participantIndices;
    }

    private List<Integer> resolveMiserieWinnerIndices(Round round, List<Player> roundPlayers, BidType bidType) {
        List<Player> miserieWinners = round.getCountMiserieWinners();
        if (bidType.getCategory() == BidCategory.MISERIE && miserieWinners.isEmpty() && round.isFinished()) {
            miserieWinners = round.getWinningPlayers();
        }
        List<Integer> indices = miserieWinners.stream().map(roundPlayers::indexOf).toList();
        if (indices.stream().anyMatch(i -> i < 0))
            throw new IllegalStateException("Cannot snapshot round: miserie winners contain players outside the round");
        return indices;
    }

    private int resolveTricksWon(Round round, BidType bidType) {
        int tricksWon = round.getCountTricksWon();
        if (tricksWon < 0) tricksWon = round.getBiddingTeamTricksWon();

        if (bidType == BidType.PASS && tricksWon != -1)
            throw new IllegalStateException("Cannot snapshot round: round passed play phase with all pass and should return tricksWon = -1");
        if (tricksWon < 0 || tricksWon > 13)
            throw new IllegalStateException("Cannot snapshot round: invalid trick count " + tricksWon);
        return tricksWon;
    }

    // =================================================================================
    // Restore
    // =================================================================================

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

        if (snapshot.mode() == SaveMode.GAME) game.setDeck(new Deck());

        game.setDealerPlayer(game.getAllPlayers().get(snapshot.dealerIndex()));
    }

    private void restoreRoundHistory(WhistGame game, List<RoundSnapshot> roundSnapshots) {
        if (roundSnapshots.isEmpty()) return;

        List<Player> players = game.getPlayers();
        if (players.size() != 4) throw new IllegalStateException("Cannot restore rounds without exactly 4 players");

        for (RoundSnapshot snapshot : roundSnapshots) {
            Player mainBidder = players.get(snapshot.bidderIndex());

            Round restoredRound = new Round(players, mainBidder, snapshot.multiplier());

            // Recreate the winning bid through the BidManager so subsequent queries
            // (getBids, getBidderOf, re-save) keep working on the restored round.
            Bid highestBid = restoredRound.getBidManager()
                    .placeBid(mainBidder.getId(), snapshot.bidType(), snapshot.trumpSuit());

            List<Player> participants = snapshot.participantIndices().stream()
                    .map(players::get)
                    .toList();
            List<Player> miserieWinners = snapshot.miserieWinnerIndices().stream()
                    .map(players::get)
                    .toList();

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

    // =================================================================================
    // Strategy mapping (unchanged)
    // =================================================================================

    private StrategySnapshotType toStrategyType(Strategy strategy) {
        switch (strategy) {
            case null -> throw new IllegalArgumentException("Cannot convert a null strategy");
            case HumanStrategy _ -> { return StrategySnapshotType.HUMAN; }
            case HighBotStrategy _ -> { return StrategySnapshotType.HIGH_BOT; }
            case LowBotStrategy _ -> { return StrategySnapshotType.LOW_BOT; }
            case SmartBotStrategy _ -> { return StrategySnapshotType.SMART_BOT; }
        }
    }

    private Strategy toStrategy(StrategySnapshotType strategyType, PlayerId restoredId) {
        return switch (strategyType) {
            case HUMAN -> new HumanStrategy();
            case HIGH_BOT -> new HighBotStrategy();
            case LOW_BOT -> new LowBotStrategy();
            case SMART_BOT -> new SmartBotStrategy();
        };
    }
}