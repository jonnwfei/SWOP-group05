package base.storage;

import base.domain.WhistGame;
import base.domain.bid.*;
import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.round.Round;
import base.domain.strategy.HighBotStrategy;
import base.domain.strategy.HumanStrategy;
import base.domain.strategy.LowBotStrategy;
import base.domain.strategy.SmartBotStrategy;
import base.domain.snapshots.GameSnapshot;
import base.domain.snapshots.PlayerSnapshot;
import base.domain.snapshots.RoundSnapshot;
import base.domain.snapshots.SaveMode;
import base.domain.snapshots.StrategySnapshotType;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.mockito.ArgumentCaptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

        when(mockGame.getAllPlayers()).thenReturn(fourPlayers);
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

    private Round createValidRound() {
        Round round = new Round(fourPlayers, p1, 1);
        Bid soloBid = round.getBidManager().placeBid(p1.getId(), BidType.SOLO, Suit.HEARTS);
        round.resolveManualCount(soloBid, List.of(p1), 13, List.of());
        return round;
    }

    private List<PlayerSnapshot> createValidPlayerSnapshots() {
        return List.of(
                new PlayerSnapshot(p1.getId().id().toString(),"P1", StrategySnapshotType.HUMAN, 0),
                new PlayerSnapshot(p2.getId().id().toString(),"P2", StrategySnapshotType.HUMAN, 0),
                new PlayerSnapshot(p3.getId().id().toString(),"P3", StrategySnapshotType.HUMAN, 0),
                new PlayerSnapshot(p4.getId().id().toString(),"P4", StrategySnapshotType.HUMAN, 0)
        );
    }

    @Nested
    @DisplayName("Constructor Constraints")
    class ConstructorTests {

        @Test
        @DisplayName("Rejects initialization with a null repository")
        void shouldRejectNullRepository() {
            assertThrows(IllegalArgumentException.class, () -> new GamePersistenceService(null));
        }

        @Test
        @DisplayName("Initializes successfully with the default repository and safely verifies directory creation")
        void shouldInitializeWithDefaultRepository() throws Exception {
            Path defaultPath = Paths.get("saves");

            // 1. RECORD: Did the folder already exist before this test started?
            boolean existedBefore = Files.exists(defaultPath);

            // Act: Using the actual default constructor
            GamePersistenceService service = new GamePersistenceService();

            assertNotNull(service);

            // Assert: Default saves path must exist
            assertTrue(Files.exists(defaultPath), "The 'saves' directory should exist.");
            assertTrue(Files.isDirectory(defaultPath), "The path should be a directory, not a file.");

            // 2. CLEANUP: Only delete the folder if THIS test was the one that created it!
            if (!existedBefore) {
                // Since this specific test doesn't save any actual games,
                // the folder will be empty and safe to delete.
                Files.deleteIfExists(defaultPath);
            }
        }
    }

    @Nested
    @DisplayName("Save Game & Player Data Serialization")
    class SaveGameTests {

        @BeforeEach
        void setupSaveGameTests() {
            when(mockGame.toSnapshot()).thenCallRealMethod();
        }

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
            assertEquals(15, savedSnapshot.players().getFirst().score());
        }

        @Test
        @DisplayName("Successfully maps complex strategy types (HighBot, LowBot, SmartBot)")
        void shouldMapStrategyTypesCorrectly() {
            Player highBot = new Player(new HighBotStrategy(), "HighBot", new PlayerId());
            Player lowBot = new Player(new LowBotStrategy(), "LowBot", new PlayerId());
            Player smartBot = new Player(new SmartBotStrategy(new PlayerId()), "SmartBot", new PlayerId());

            when(mockGame.getAllPlayers()).thenReturn(List.of(highBot, lowBot, smartBot, p4));
            when(mockGame.getDealerPlayer()).thenReturn(highBot); // highBot is at index 0

            persistenceService.save(mockGame, SaveMode.GAME, "Bot Save");

            ArgumentCaptor<GameSnapshot> captor = ArgumentCaptor.forClass(GameSnapshot.class);
            verify(mockRepository).save(captor.capture());
            GameSnapshot saved = captor.getValue();

            assertEquals(StrategySnapshotType.HIGH_BOT, saved.players().get(0).strategyType());
            assertEquals(StrategySnapshotType.LOW_BOT, saved.players().get(1).strategyType());
            assertEquals(StrategySnapshotType.SMART_BOT, saved.players().get(2).strategyType());
        }

        @Test
        @DisplayName("Throws when attempting to map a null strategy")
        void shouldThrowWhenStrategyIsNull() {
            Player corruptedPlayer = mock(Player.class);
            when(corruptedPlayer.getId()).thenReturn(new PlayerId());
            when(corruptedPlayer.getName()).thenReturn("Corrupted");
            when(corruptedPlayer.getDecisionStrategy()).thenReturn(null); // The culprit

            when(mockGame.getAllPlayers()).thenReturn(List.of(corruptedPlayer, p2, p3, p4));
            when(mockGame.getDealerPlayer()).thenReturn(corruptedPlayer);

            assertThrows(IllegalArgumentException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));
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

            // Create a dealer who is not in the list of the game's players
            Player ghostDealer = new Player(new HumanStrategy(), "Ghost", new PlayerId());
            when(mockGame.getDealerPlayer()).thenReturn(ghostDealer);
            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Ghost Save"));
        }
    }
    @Nested
    @DisplayName("Round Serialization (toSnapshot)")
    class RoundSerializationTests {

        @Test
        @DisplayName("Successfully serializes a valid round into a RoundSnapshot")
        void shouldSerializeValidRoundSuccessfully() {
            Round validRound = createValidRound();
            when(mockGame.getRounds()).thenReturn(List.of(validRound));
            when(mockGame.toSnapshot()).thenCallRealMethod();

            persistenceService.save(mockGame, SaveMode.GAME, "Round Save");
            verify(mockRepository).save(any(GameSnapshot.class));
        }

        @Test
        @DisplayName("Rejects corrupted round structures")
        void shouldThrowOnCorruptedRoundElements() {
            when(mockGame.toSnapshot()).thenCallRealMethod();

            when(mockGame.getRounds()).thenReturn(Collections.singletonList(null));
            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));
        }

        @Test
        @DisplayName("Rejects PASS round if tricksWon is not -1")
        void shouldRejectPassRoundWithInvalidTricks() {
            Round round = new Round(fourPlayers, p1, 1);
            Bid passBid = round.getBidManager().placeBid(p1.getId(), BidType.PASS, null);

            // Force invalid tricks (5) for a PASS bid
            round.resolveManualCount(passBid, List.of(), 5, List.of());

            when(mockGame.getRounds()).thenReturn(List.of(round));
            when(mockGame.toSnapshot()).thenCallRealMethod();

            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));
        }

        @Test
        @DisplayName("Catches inner RoundSnapshot Validation Exceptions and wraps them in IllegalStateException")
        void shouldWrapRoundSnapshotValidationExceptions() throws Exception {
            Round round = createValidRound();

            // Force invalid Deltas (Sums to +10) via reflection to bypass domain logic
            java.lang.reflect.Field deltaField = Round.class.getDeclaredField("scoreDeltas");
            deltaField.setAccessible(true);
            deltaField.set(round, new ArrayList<>(List.of(90, -30, -30, -20)));

            when(mockGame.getRounds()).thenReturn(List.of(round));
            when(mockGame.toSnapshot()).thenCallRealMethod();

            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));
            assertTrue(ex.getMessage().contains("Round contains invalid data"));
        }

        @Test
        @DisplayName("Rejects rounds with invalid player counts")
        void shouldThrowOnInvalidPlayerCount() throws Exception {
            Round round = createValidRound();

            // Force invalid player count via reflection
            java.lang.reflect.Field playersField = Round.class.getDeclaredField("players");
            playersField.setAccessible(true);
            playersField.set(round, List.of(p1, p2, p3));

            when(mockGame.getRounds()).thenReturn(List.of(round));
            when(mockGame.toSnapshot()).thenCallRealMethod();

            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));
        }

        @Test
        @DisplayName("Rejects rounds where participants or bidders are not in the round")
        void shouldThrowOnUnknownPlayers() {
            Round round = new Round(fourPlayers, p1, 1);
            Player alienPlayer = new Player(new HumanStrategy(), "Alien", new PlayerId());
            Bid soloBid = round.getBidManager().placeBid(p1.getId(), BidType.SOLO, Suit.HEARTS);

            when(mockGame.toSnapshot()).thenCallRealMethod();

            // 1. Participant not in the game
            round.resolveManualCount(soloBid, List.of(alienPlayer), 13, List.of());
            when(mockGame.getRounds()).thenReturn(List.of(round));
            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));
        }

        @Test
        @DisplayName("Rejects rounds with invalid miserie winners")
        void shouldThrowOnInvalidMiserieWinners() {
            Round round = new Round(fourPlayers, p1, 1);
            Bid miserieBid = round.getBidManager().placeBid(p1.getId(), BidType.MISERIE, null);
            Player alienPlayer = new Player(new HumanStrategy(), "Alien", new PlayerId());

            // Alien player as miserie winner
            round.resolveManualCount(miserieBid, List.of(p1), -1, List.of(alienPlayer));

            when(mockGame.getRounds()).thenReturn(List.of(round));
            when(mockGame.toSnapshot()).thenCallRealMethod();

            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));
        }

        @Test
        @DisplayName("Rejects rounds with invalid score deltas (null, or wrong size)")
        void shouldThrowOnInvalidDeltas() throws Exception {
            Round round = createValidRound();

            when(mockGame.getRounds()).thenReturn(List.of(round));
            when(mockGame.toSnapshot()).thenCallRealMethod();

            // Test wrong size
            java.lang.reflect.Field deltaField = Round.class.getDeclaredField("scoreDeltas");
            deltaField.setAccessible(true);
            deltaField.set(round, new ArrayList<>(List.of(90, -30, -30)));
            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));

            // Test null
            deltaField.set(round, null);
            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));
        }

        @Test
        @DisplayName("Coverage test - Resolve constraints in RoundSnapshot via toSnapshot")
        void shouldCoverageResolveTricksWon() {
            Round round = new Round(fourPlayers, p1, 1);
            Bid soloBid = round.getBidManager().placeBid(p1.getId(), BidType.SOLO, null);

            when(mockGame.toSnapshot()).thenCallRealMethod();
            when(mockGame.getRounds()).thenReturn(List.of(round));

            // --- Test 1: SoloBid (-14) ---
            round.resolveManualCount(soloBid, List.of(p1), -14, List.of());
            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));

            // --- Test 2: SoloBid (14) ---
            round.resolveManualCount(soloBid, List.of(p1), 14, List.of());
            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));
        }

        @Test
        @DisplayName("Rejects non-PASS rounds with zero participants")
        void shouldThrowOnZeroParticipantsForNormalBid() {
            Round round = new Round(fourPlayers, p1, 1);
            Bid soloBid = round.getBidManager().placeBid(p1.getId(), BidType.SOLO, Suit.HEARTS);

            // Zero participants for SOLO
            round.resolveManualCount(soloBid, List.of(), 13, List.of());

            when(mockGame.getRounds()).thenReturn(List.of(round));
            when(mockGame.toSnapshot()).thenCallRealMethod();
            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));
        }
    }


    @Nested
    @DisplayName("Load Game & Restore History")
    class LoadGameTests {

        @Test
        @DisplayName("Successfully loads a GAME mode save, delegating restoration to the game")
        void shouldLoadGameModeAndRestoreFullState() {
            String description = "Saved Game";
            GameSnapshot mockSnapshot = new GameSnapshot(description, SaveMode.GAME, 0, createValidPlayerSnapshots(), List.of());
            when(mockRepository.loadByDescription(description)).thenReturn(mockSnapshot);

            // Call the service
            SaveMode resultMode = persistenceService.loadIntoGame(mockGame, description);

            // 1. Verify the service returned the correct mode
            assertEquals(SaveMode.GAME, resultMode);

            // 2. Verify the service delegated the actual work to the Game class!
            verify(mockGame).restoreGame(mockSnapshot);
            assertEquals(4, mockGame.getPlayers().size());
            assertEquals(0, mockGame.getRounds().size());
        }

        @Test
        @DisplayName("Successfully loads a COUNT mode save without restoring the deck")
        void shouldLoadCountModeWithoutDeck() {
            List<RoundSnapshot> rounds = List.of(
                    new RoundSnapshot(
                            fourPlayers.stream().map(p -> p.getId().id().toString()).toList(),BidType.PASS, 0, List.of(0), -1, List.of(), 1, List.of(0, 0, 0, 0), Suit.HEARTS)
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
        @DisplayName("Restoring rounds to a game without exactly 4 players throws IllegalStateException")
        void shouldEnforceFourPlayersDuringRoundRestoration() {
            GameSnapshot corruptedSnapshot = mock(GameSnapshot.class);
            when(corruptedSnapshot.mode()).thenReturn(SaveMode.GAME);
            when(corruptedSnapshot.dealerIndex()).thenReturn(0);

            List<PlayerSnapshot> badPlayersList = List.of(
                    new PlayerSnapshot(new PlayerId().id().toString(), "P1", StrategySnapshotType.HUMAN, 0)
            );
            when(corruptedSnapshot.players()).thenReturn(badPlayersList);

            List<RoundSnapshot> rounds = List.of(
                    new RoundSnapshot(
                            fourPlayers.stream().map(p -> p.getId().id().toString()).toList(),BidType.SOLO, 0, List.of(0), 13, List.of(), 1, List.of(90, -30, -30, -30), Suit.HEARTS)
            );
            when(corruptedSnapshot.rounds()).thenReturn(rounds);
            when(mockRepository.loadByDescription("bad_restore")).thenReturn(corruptedSnapshot);

            // 4. Use a REAL Game object, not a mock!
            WhistGame realGame = new WhistGame();

            // 5. The PersistenceService passes the snapshot to realGame.restoreGame().
            assertThrows(IllegalStateException.class, () -> persistenceService.loadIntoGame(realGame, "bad_restore"));
        }

        @Test
        @DisplayName("Successfully restores all bot strategy types during load")
        void shouldRestoreAllStrategyTypes() {
            // 1. Create a snapshot with exactly one of every strategy type
            GameSnapshot snapshot = createValidGameSnapshot();
            when(mockRepository.loadByDescription("Bot Load")).thenReturn(snapshot);

            // 2. Use a REAL Game object
            WhistGame realGame = new WhistGame();

            // 3. Act
            persistenceService.loadIntoGame(realGame, "Bot Load");

            // 4. Get the players that were successfully added to the real game
            List<Player> restoredPlayers = realGame.getPlayers();

            // 5. Assert: Verify every single strategy was mapped back to the correct Domain Strategy class
            assertEquals(4, restoredPlayers.size());
            assertInstanceOf(HumanStrategy.class, restoredPlayers.get(0).getDecisionStrategy());
            assertInstanceOf(HighBotStrategy.class, restoredPlayers.get(1).getDecisionStrategy());
            assertInstanceOf(LowBotStrategy.class, restoredPlayers.get(2).getDecisionStrategy());
            assertInstanceOf(SmartBotStrategy.class, restoredPlayers.get(3).getDecisionStrategy());
        }
    }

    // Keep your original method for tests that don't care about rounds
    private static GameSnapshot createValidGameSnapshot() {
        return createValidGameSnapshot(List.of());
    }

    // Add an overloaded method that accepts a custom list of rounds
    private static GameSnapshot createValidGameSnapshot(List<RoundSnapshot> rounds) {
        List<PlayerSnapshot> variedPlayers = List.of(
                new PlayerSnapshot(new PlayerId().id().toString(), "P1", StrategySnapshotType.HUMAN, 0),
                new PlayerSnapshot(new PlayerId().id().toString(), "P2", StrategySnapshotType.HIGH_BOT, 0),
                new PlayerSnapshot(new PlayerId().id().toString(), "P3", StrategySnapshotType.LOW_BOT, 0),
                new PlayerSnapshot(new PlayerId().id().toString(), "P4", StrategySnapshotType.SMART_BOT, 0)
        );
        // This guarantees the GameSnapshot constructor receives exactly 4 players, preventing the exception
        return new GameSnapshot("Bot Load", SaveMode.GAME, 0, variedPlayers, rounds);
    }

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