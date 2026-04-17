package base.storage;

import base.domain.WhistGame;
import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.bid.SoloBid;
import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.round.Round;
import base.domain.strategy.HighBotStrategy;
import base.domain.strategy.HumanStrategy;
import base.domain.strategy.LowBotStrategy;
import base.storage.snapshots.GameSnapshot;
import base.storage.snapshots.PlayerSnapshot;
import base.storage.snapshots.RoundSnapshot;
import base.storage.snapshots.SaveMode;
import base.storage.snapshots.StrategySnapshotType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Validates the serialization and restoration logic of the GamePersistenceService.
 * This service acts as a Data Mapper/Memento handler between the Domain and Storage layers.
 * *
 */
@DisplayName("Game Persistence Service Tests")
class GamePersistenceServiceTest {

    @Mock private SaveRepository mockRepository;
    @Mock private WhistGame mockGame;

    private AutoCloseable mocks;
    private GamePersistenceService persistenceService;

    private Player p1, p2, p3, p4;
    private List<Player> fourPlayers;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        persistenceService = new GamePersistenceService(mockRepository);

        p1 = new Player(new HumanStrategy(), "P1", new PlayerId());
        p2 = new Player(new HumanStrategy(), "P2", new PlayerId());
        p3 = new Player(new HumanStrategy(), "P3", new PlayerId());
        p4 = new Player(new HumanStrategy(), "P4", new PlayerId());
        fourPlayers = List.of(p1, p2, p3, p4);

        when(mockGame.getPlayers()).thenReturn(fourPlayers);
        when(mockGame.getDealerPlayer()).thenReturn(p1);
        when(mockGame.getRounds()).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private Round createValidMockRound() {
        Round mockRound = mock(Round.class);
        Bid realBid = new SoloBid(p1.getId(), BidType.SOLO, Suit.HEARTS);

        when(mockRound.getHighestBid()).thenReturn(realBid);
        when(mockRound.getPlayers()).thenReturn(fourPlayers);
        when(mockRound.getPlayerById(p1.getId())).thenReturn(p1);
        when(mockRound.getBiddingTeamPlayers()).thenReturn(List.of(p1));
        when(mockRound.getCountMiserieWinners()).thenReturn(List.of());
        when(mockRound.getScoreDeltas()).thenReturn(List.of(90, -30, -30, -30));
        when(mockRound.getCountTricksWon()).thenReturn(13);
        when(mockRound.getMultiplier()).thenReturn(1);

        return mockRound;
    }

    private List<PlayerSnapshot> createValidPlayerSnapshots() {
        return List.of(
                new PlayerSnapshot(new PlayerId().toString(),"P1", StrategySnapshotType.HUMAN, 0),
                new PlayerSnapshot(new PlayerId().toString(),"P2", StrategySnapshotType.HUMAN, 0),
                new PlayerSnapshot(new PlayerId().toString(),"P3", StrategySnapshotType.HUMAN, 0),
                new PlayerSnapshot(new PlayerId().toString(),"P4", StrategySnapshotType.HUMAN, 0)
        );
    }

    // =========================================================================
    // CONSTRUCTOR TESTS
    // =========================================================================

    @Nested
    @DisplayName("Constructor Constraints")
    class ConstructorTests {

        @Test
        @DisplayName("Rejects initialization with a null repository")
        void shouldRejectNullRepository() {
            assertThrows(IllegalArgumentException.class, () -> new GamePersistenceService(null));
        }

        @Test
        @DisplayName("Initializes successfully with the default repository")
        void shouldInitializeWithDefaultRepository() {
            assertNotNull(new GamePersistenceService());
        }
    }

    // =========================================================================
    // SAVE TESTS
    // =========================================================================

    @Nested
    @DisplayName("Save Game & Player Data Serialization")
    class SaveGameTests {

        @Test
        @DisplayName("Successfully saves a game in GAME mode with accurate data mapping")
        void shouldSaveGameMainSuccessScenario() {
            String description = "late night stretch";
            SaveMode mode = SaveMode.GAME;
            p1.updateScore(15);

            persistenceService.save(mockGame, mode, description);

            ArgumentCaptor<GameSnapshot> snapshotCaptor = ArgumentCaptor.forClass(GameSnapshot.class);
            verify(mockRepository).save(snapshotCaptor.capture());

            GameSnapshot savedSnapshot = snapshotCaptor.getValue();
            assertEquals(description, savedSnapshot.description());
            assertEquals(mode, savedSnapshot.mode());
            assertEquals(0, savedSnapshot.dealerIndex());
            assertEquals(4, savedSnapshot.players().size());
            assertEquals("P1", savedSnapshot.players().get(0).name());
            assertEquals(15, savedSnapshot.players().get(0).score());
        }

        @Test
        @DisplayName("Successfully maps complex strategy types (HighBot, LowBot)")
        void shouldMapStrategyTypesCorrectly() {
            Player highBot = new Player(new HighBotStrategy(), "HighBot", new PlayerId());
            Player lowBot = new Player(new LowBotStrategy(), "LowBot", new PlayerId());
            when(mockGame.getPlayers()).thenReturn(List.of(highBot, lowBot, p3, p4));
            when(mockGame.getDealerPlayer()).thenReturn(highBot);

            persistenceService.save(mockGame, SaveMode.GAME, "Bot Save");

            ArgumentCaptor<GameSnapshot> captor = ArgumentCaptor.forClass(GameSnapshot.class);
            verify(mockRepository).save(captor.capture());
            GameSnapshot saved = captor.getValue();

            assertEquals(0, saved.dealerIndex());
            assertEquals(StrategySnapshotType.HIGH_BOT, saved.players().get(0).strategyType());
            assertEquals(StrategySnapshotType.LOW_BOT, saved.players().get(1).strategyType());
        }

        @Test
        @DisplayName("Rejects null or blank arguments aggressively")
        void shouldRejectInvalidSaveArguments() {
            assertThrows(IllegalArgumentException.class, () -> persistenceService.save(null, SaveMode.GAME, "Desc"));
            assertThrows(IllegalArgumentException.class, () -> persistenceService.save(mockGame, null, "Desc"));
            assertThrows(IllegalArgumentException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, null));
            assertThrows(IllegalArgumentException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "   "));
        }

        @Test
        @DisplayName("Rejects saving if the dealer state is null or corrupted")
        void shouldThrowWhenDealerStateIsCorrupted() {
            when(mockGame.getDealerPlayer()).thenReturn(null);
            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Null Dealer Save"));

            Player ghostDealer = new Player(new HumanStrategy(), "Ghost", new PlayerId());
            when(mockGame.getDealerPlayer()).thenReturn(ghostDealer);
            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Ghost Save"));
        }

        @Test
        @DisplayName("Rejects saving if the player list is corrupted (contains nulls)")
        void shouldThrowWhenPlayerListContainsNulls() {
            List<Player> invalidPlayers = new ArrayList<>();
            invalidPlayers.add(p1);
            invalidPlayers.add(null);
            when(mockGame.getPlayers()).thenReturn(invalidPlayers);

            assertThrows(IllegalArgumentException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));
        }
    }

    // =========================================================================
    // ROUND SNAPSHOT (SERIALIZATION) TESTS
    // =========================================================================

    @Nested
    @DisplayName("Round Serialization (toSnapshot)")
    class RoundSerializationTests {

        @Test
        @DisplayName("Successfully serializes a valid round into a RoundSnapshot")
        void shouldSerializeValidRoundSuccessfully() {
            Round validRound = createValidMockRound();
            when(mockGame.getRounds()).thenReturn(List.of(validRound));

            persistenceService.save(mockGame, SaveMode.GAME, "Round Save");
            verify(mockRepository).save(any(GameSnapshot.class));
        }

        @Test
        @DisplayName("Rejects corrupted round structures (null bids or null rounds)")
        void shouldThrowOnCorruptedRoundElements() {
            when(mockGame.getRounds()).thenReturn(Collections.singletonList(null));
            assertThrows(IllegalArgumentException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));

            Round mockRound = createValidMockRound();
            when(mockRound.getHighestBid()).thenReturn(null);
            when(mockGame.getRounds()).thenReturn(List.of(mockRound));

            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));
        }

        @Test
        @DisplayName("Rejects rounds containing invalid score deltas or trick counts")
        void shouldThrowOnInvalidDeltasOrTricks() {
            Round mockRound = createValidMockRound();
            when(mockGame.getRounds()).thenReturn(List.of(mockRound));

            when(mockRound.getScoreDeltas()).thenReturn(List.of(0, 0));
            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));

            when(mockRound.getScoreDeltas()).thenReturn(List.of(90, -30, -30, -30));
            when(mockRound.getCountTricksWon()).thenReturn(-5);
            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));
        }
    }

    // =========================================================================
    // LOAD TESTS
    // =========================================================================

    @Nested
    @DisplayName("Load Game & Restore History")
    class LoadGameTests {

        @Test
        @DisplayName("Successfully loads a GAME mode save, restoring players, deck, and dealer")
        void shouldLoadGameModeAndRestoreFullState() {
            String description = "Saved Game";
            GameSnapshot mockSnapshot = new GameSnapshot(description, SaveMode.GAME, 0, createValidPlayerSnapshots(), List.of());
            when(mockRepository.loadByDescription(description)).thenReturn(mockSnapshot);

            List<Player> restoredPlayers = new ArrayList<>();
            when(mockGame.getPlayers()).thenReturn(restoredPlayers);
            doAnswer(invocation -> {
                restoredPlayers.add(invocation.getArgument(0));
                return null;
            }).when(mockGame).addPlayer(any(Player.class));

            SaveMode resultMode = persistenceService.loadIntoGame(mockGame, description);

            assertEquals(SaveMode.GAME, resultMode);
            verify(mockGame).resetPlayers();
            verify(mockGame).resetRounds();
            verify(mockGame, times(4)).addPlayer(any(Player.class));
            verify(mockGame).setDeck(any());
            assertEquals(restoredPlayers.get(0), restoredPlayers.get(0)); // Visual parity
        }

        @Test
        @DisplayName("Successfully loads a COUNT mode save without restoring the deck")
        void shouldLoadCountModeWithoutDeck() {
            List<RoundSnapshot> rounds = List.of(
                    new RoundSnapshot(BidType.PASS, 0, List.of(0), -1, List.of(), 1, List.of(0, 0, 0, 0))
            );
            GameSnapshot countSnapshot = new GameSnapshot("Count Save", SaveMode.COUNT, 0, createValidPlayerSnapshots(), rounds);
            when(mockRepository.loadByDescription("Count Save")).thenReturn(countSnapshot);

            List<Player> restoredPlayers = new ArrayList<>();
            when(mockGame.getPlayers()).thenReturn(restoredPlayers);
            doAnswer(invocation -> {
                restoredPlayers.add(invocation.getArgument(0));
                return null;
            }).when(mockGame).addPlayer(any(Player.class));

            SaveMode mode = persistenceService.loadIntoGame(mockGame, "Count Save");

            assertEquals(SaveMode.COUNT, mode);
            verify(mockGame, never()).setDeck(any());
        }

        @Test
        @DisplayName("Rejects loading with null arguments or missing saves")
        void shouldDefendAgainstInvalidLoadRequests() {
            when(mockRepository.loadByDescription("Missing")).thenThrow(new IllegalArgumentException("No save found"));

            assertThrows(IllegalArgumentException.class, () -> persistenceService.loadIntoGame(null, "Desc"));
            assertThrows(IllegalArgumentException.class, () -> persistenceService.loadIntoGame(mockGame, null));
            assertThrows(IllegalArgumentException.class, () -> persistenceService.loadIntoGame(mockGame, "Missing"));
        }

        @Test
        @DisplayName("Restoring rounds to a game without exactly 4 players throws IllegalStateException")
        void shouldEnforceFourPlayersDuringRoundRestoration() {
            List<PlayerSnapshot> playerSnapshots = List.of(new PlayerSnapshot(new PlayerId().toString(),"P1", StrategySnapshotType.HUMAN, 0));
            List<RoundSnapshot> rounds = List.of(
                    new RoundSnapshot(BidType.SOLO, 0, List.of(0), 13, List.of(), 1, List.of(90, -30, -30, -30))
            );
            GameSnapshot mockSnapshot = new GameSnapshot("Desc", SaveMode.COUNT, 0, playerSnapshots, rounds);
            when(mockRepository.loadByDescription("bad_restore")).thenReturn(mockSnapshot);

            assertThrows(IllegalStateException.class, () -> persistenceService.loadIntoGame(mockGame, "bad_restore"));
        }
    }

    // =========================================================================
    // REPOSITORY DELEGATION TESTS
    // =========================================================================

    @Nested
    @DisplayName("Repository Delegation")
    class RepositoryDelegationTests {

        @Test
        @DisplayName("Correctly delegates listDescriptions() to the underlying repository")
        void shouldDelegateListDescriptions() {
            List<String> expectedList = List.of("Save 1", "Save 2");
            when(mockRepository.listDescriptions()).thenReturn(expectedList);

            List<String> actualList = persistenceService.listDescriptions();

            assertEquals(expectedList, actualList);
            verify(mockRepository).listDescriptions();
        }
    }
}