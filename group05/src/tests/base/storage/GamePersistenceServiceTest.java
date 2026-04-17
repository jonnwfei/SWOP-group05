package base.storage;

import base.domain.WhistGame;
import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.bid.SoloBid;
import base.domain.card.Suit;
import base.domain.player.Player;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("Game Persistence Service Tests")
class GamePersistenceServiceTest {

    @Mock private SaveRepository mockRepository;
    @Mock private WhistGame mockGame;

    private AutoCloseable mocks; // Used for manual lifecycle management
    private GamePersistenceService persistenceService;

    // Standard 4-player setup
    private Player p1, p2, p3, p4;
    private List<Player> fourPlayers;

    @BeforeEach
    void setUp() {
        // Initialize all fields annotated with @Mock
        mocks = MockitoAnnotations.openMocks(this);

        persistenceService = new GamePersistenceService(mockRepository);

        p1 = new Player(new HumanStrategy(), "P1");
        p2 = new Player(new HumanStrategy(), "P2");
        p3 = new Player(new HumanStrategy(), "P3");
        p4 = new Player(new HumanStrategy(), "P4");
        fourPlayers = List.of(p1, p2, p3, p4);

        // Default mock behaviors (Mockito is lenient by default without the extension)
        when(mockGame.getPlayers()).thenReturn(fourPlayers);
        when(mockGame.getDealerPlayer()).thenReturn(p1);
        when(mockGame.getRounds()).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() throws Exception {
        // Clean up the mocks to prevent memory leaks across tests
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
                new PlayerSnapshot("P1", StrategySnapshotType.HUMAN, 0),
                new PlayerSnapshot("P2", StrategySnapshotType.HUMAN, 0),
                new PlayerSnapshot("P3", StrategySnapshotType.HUMAN, 0),
                new PlayerSnapshot("P4", StrategySnapshotType.HUMAN, 0)
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
            assertThatThrownBy(() -> new GamePersistenceService(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null repository");
        }

        @Test
        @DisplayName("Initializes successfully with the default repository")
        void shouldInitializeWithDefaultRepository() {
            new GamePersistenceService();
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
            assertThat(savedSnapshot.description()).isEqualTo(description);
            assertThat(savedSnapshot.mode()).isEqualTo(mode);
            assertThat(savedSnapshot.dealerIndex()).isZero();
            assertThat(savedSnapshot.players()).hasSize(4);
            assertThat(savedSnapshot.players().get(0).name()).isEqualTo("P1");
            assertThat(savedSnapshot.players().get(0).score()).isEqualTo(15);
        }

        @Test
        @DisplayName("Successfully maps complex strategy types (HighBot, LowBot)")
        void shouldMapStrategyTypesCorrectly() {
            Player highBot = new Player(new HighBotStrategy(), "HighBot");
            Player lowBot = new Player(new LowBotStrategy(), "LowBot");
            when(mockGame.getPlayers()).thenReturn(List.of(highBot, lowBot, p3, p4));
            when(mockGame.getDealerPlayer()).thenReturn(highBot);

            persistenceService.save(mockGame, SaveMode.GAME, "Bot Save");

            ArgumentCaptor<GameSnapshot> captor = ArgumentCaptor.forClass(GameSnapshot.class);
            verify(mockRepository).save(captor.capture());
            GameSnapshot saved = captor.getValue();

            assertThat(saved.dealerIndex()).isZero();
            assertThat(saved.players().get(0).strategyType()).isEqualTo(StrategySnapshotType.HIGH_BOT);
            assertThat(saved.players().get(1).strategyType()).isEqualTo(StrategySnapshotType.LOW_BOT);
        }

        @Test
        @DisplayName("Rejects null or blank arguments aggressively")
        void shouldRejectInvalidSaveArguments() {
            assertThatThrownBy(() -> persistenceService.save(null, SaveMode.GAME, "Desc"))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> persistenceService.save(mockGame, null, "Desc"))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> persistenceService.save(mockGame, SaveMode.GAME, null))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> persistenceService.save(mockGame, SaveMode.GAME, "   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Rejects saving if the dealer state is null or corrupted")
        void shouldThrowWhenDealerStateIsCorrupted() {
            when(mockGame.getDealerPlayer()).thenReturn(null);
            assertThatThrownBy(() -> persistenceService.save(mockGame, SaveMode.GAME, "Null Dealer Save"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("null dealer player");

            Player ghostDealer = new Player(new HumanStrategy(), "Ghost");
            when(mockGame.getDealerPlayer()).thenReturn(ghostDealer);
            assertThatThrownBy(() -> persistenceService.save(mockGame, SaveMode.GAME, "Ghost Save"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("part of the current players list");
        }

        @Test
        @DisplayName("Rejects saving if the player list is corrupted (contains nulls)")
        void shouldThrowWhenPlayerListContainsNulls() {
            List<Player> invalidPlayers = new ArrayList<>();
            invalidPlayers.add(p1);
            invalidPlayers.add(null);
            when(mockGame.getPlayers()).thenReturn(invalidPlayers);

            assertThatThrownBy(() -> persistenceService.save(mockGame, SaveMode.GAME, "Test"))
                    .isInstanceOf(IllegalArgumentException.class);
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
            assertThatThrownBy(() -> persistenceService.save(mockGame, SaveMode.GAME, "Test"))
                    .isInstanceOf(IllegalArgumentException.class);

            Round mockRound = createValidMockRound();
            when(mockRound.getHighestBid()).thenReturn(null);
            when(mockGame.getRounds()).thenReturn(List.of(mockRound));

            assertThatThrownBy(() -> persistenceService.save(mockGame, SaveMode.GAME, "Test"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("highest bid");
        }

        @Test
        @DisplayName("Rejects rounds containing invalid score deltas or trick counts")
        void shouldThrowOnInvalidDeltasOrTricks() {
            Round mockRound = createValidMockRound();
            when(mockGame.getRounds()).thenReturn(List.of(mockRound));

            when(mockRound.getScoreDeltas()).thenReturn(List.of(0, 0));
            assertThatThrownBy(() -> persistenceService.save(mockGame, SaveMode.GAME, "Test"))
                    .isInstanceOf(IllegalStateException.class);

            when(mockRound.getScoreDeltas()).thenReturn(List.of(90, -30, -30, -30));
            when(mockRound.getCountTricksWon()).thenReturn(-5);
            when(mockRound.getBiddingTeamTricksWon()).thenReturn(-2);
            assertThatThrownBy(() -> persistenceService.save(mockGame, SaveMode.GAME, "Test"))
                    .isInstanceOf(IllegalStateException.class);
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

            assertThat(resultMode).isEqualTo(SaveMode.GAME);
            verify(mockGame).resetPlayers();
            verify(mockGame).resetRounds();
            verify(mockGame, times(4)).addPlayer(any(Player.class));
            verify(mockGame).setDeck(any());
            verify(mockGame).setDealerPlayer(restoredPlayers.get(0));
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

            assertThat(mode).isEqualTo(SaveMode.COUNT);
            verify(mockGame, never()).setDeck(any());
            verify(mockGame).setDealerPlayer(restoredPlayers.get(0));
        }

        @Test
        @DisplayName("Rejects loading with null arguments or missing saves")
        void shouldDefendAgainstInvalidLoadRequests() {
            when(mockRepository.loadByDescription("Missing")).thenThrow(new IllegalArgumentException("No save found"));

            assertThatThrownBy(() -> persistenceService.loadIntoGame(null, "Desc"))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> persistenceService.loadIntoGame(mockGame, null))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> persistenceService.loadIntoGame(mockGame, "Missing"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Restoring rounds to a game without exactly 4 players throws IllegalStateException")
        void shouldEnforceFourPlayersDuringRoundRestoration() {
            List<PlayerSnapshot> playerSnapshots = List.of(new PlayerSnapshot("P1", StrategySnapshotType.HUMAN, 0));
            List<RoundSnapshot> rounds = List.of(
                    new RoundSnapshot(BidType.SOLO, 0, List.of(0), 13, List.of(), 1, List.of(90, -30, -30, -30))
            );
            GameSnapshot mockSnapshot = new GameSnapshot("Desc", SaveMode.COUNT, 0, playerSnapshots, rounds);
            when(mockRepository.loadByDescription("bad_restore")).thenReturn(mockSnapshot);

            List<Player> restoredPlayers = new ArrayList<>();
            when(mockGame.getPlayers()).thenReturn(restoredPlayers);
            doAnswer(invocation -> {
                restoredPlayers.add(invocation.getArgument(0));
                return null;
            }).when(mockGame).addPlayer(any(Player.class));

            assertThatThrownBy(() -> persistenceService.loadIntoGame(mockGame, "bad_restore"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("exactly 4 players");
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

            assertThat(actualList).isEqualTo(expectedList);
            verify(mockRepository).listDescriptions();
        }
    }
}