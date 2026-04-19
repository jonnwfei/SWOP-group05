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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private Round createValidMockRound() {
        Round mockRound = mock(Round.class);
        Bid realBid = new SoloBid(p1.getId(), BidType.SOLO, Suit.HEARTS);

        when(mockRound.getHighestBid()).thenReturn(realBid);
        when(mockRound.getPlayers()).thenReturn(fourPlayers);
        when(mockRound.getPlayerById(any())).thenReturn(p1);
        when(mockRound.getBiddingTeamPlayers()).thenReturn(List.of(p1));
        when(mockRound.getCountMiserieWinners()).thenReturn(List.of());
        when(mockRound.getScoreDeltas()).thenReturn(List.of(90, -30, -30, -30));
        when(mockRound.getCountTricksWon()).thenReturn(13);
        when(mockRound.getMultiplier()).thenReturn(1);

        return mockRound;
    }

    private List<PlayerSnapshot> createValidPlayerSnapshots() {
        return List.of(
                new PlayerSnapshot(new PlayerId().id().toString(),"P1", StrategySnapshotType.HUMAN, 0),
                new PlayerSnapshot(new PlayerId().id().toString(),"P2", StrategySnapshotType.HUMAN, 0),
                new PlayerSnapshot(new PlayerId().id().toString(),"P3", StrategySnapshotType.HUMAN, 0),
                new PlayerSnapshot(new PlayerId().id().toString(),"P4", StrategySnapshotType.HUMAN, 0)
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
            when(mockGame.getDealerPlayer()).thenReturn(highBot);

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
            Round validRound = createValidMockRound();
            when(mockGame.getRounds()).thenReturn(List.of(validRound));

            persistenceService.save(mockGame, SaveMode.GAME, "Round Save");
            verify(mockRepository).save(any(GameSnapshot.class));
        }

        @Test
        @DisplayName("Rejects corrupted round structures")
        void shouldThrowOnCorruptedRoundElements() {
            when(mockGame.getRounds()).thenReturn(Collections.singletonList(null));
            assertThrows(IllegalArgumentException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));

            Round mockRound = createValidMockRound();
            when(mockRound.getHighestBid()).thenReturn(null);
            when(mockGame.getRounds()).thenReturn(List.of(mockRound));
            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));

            // Round with missing participants for a non-PASS bid
            when(mockRound.getHighestBid()).thenReturn(new SoloBid(p1.getId(), BidType.SOLO, Suit.HEARTS));
            when(mockRound.getBiddingTeamPlayers()).thenReturn(List.of());
            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));
        }

        @Test
        @DisplayName("Rejects PASS round if tricksWon is not -1")
        void shouldRejectPassRoundWithInvalidTricks() {
            Round mockRound = createValidMockRound();
            when(mockRound.getHighestBid()).thenReturn(new PassBid(p1.getId()));
            when(mockRound.getCountTricksWon()).thenReturn(5); // Should strictly be -1 for PASS

            when(mockGame.getRounds()).thenReturn(List.of(mockRound));

            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));
        }

        @Test
        @DisplayName("Catches inner RoundSnapshot Validation Exceptions and wraps them in IllegalStateException")
        void shouldWrapRoundSnapshotValidationExceptions() {
            Round mockRound = createValidMockRound();
            // Invalid Deltas (Sums to +10, triggering the RoundSnapshot IllegalArgumentException)
            when(mockRound.getScoreDeltas()).thenReturn(List.of(90, -30, -30, -20));

            when(mockGame.getRounds()).thenReturn(List.of(mockRound));

            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));
            assertTrue(ex.getMessage().contains("Round contains invalid data"));
        }

        @Test
        @DisplayName("Rejects rounds with invalid player counts")
        void shouldThrowOnInvalidPlayerCount() {
            Round mockRound = createValidMockRound();
            // Force the round to return only 3 players
            when(mockRound.getPlayers()).thenReturn(List.of(p1, p2, p3));
            when(mockGame.getRounds()).thenReturn(List.of(mockRound));

            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));
        }

        @Test
        @DisplayName("Rejects rounds where participants or bidders are not in the round")
        void shouldThrowOnUnknownPlayers() {
            Round mockRound = createValidMockRound();
            Player alienPlayer = new Player(new HumanStrategy(), "Alien", new PlayerId());

            // 1. Bidder not in the game
            when(mockRound.getPlayerById(any())).thenReturn(alienPlayer);
            when(mockGame.getRounds()).thenReturn(List.of(mockRound));
            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));

            // Reset bidder, now make participant an alien
            when(mockRound.getPlayerById(any())).thenReturn(p1);
            when(mockRound.getBiddingTeamPlayers()).thenReturn(List.of(alienPlayer));
            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));
        }

        @Test
        @DisplayName("Rejects rounds with invalid miserie winners")
        void shouldThrowOnInvalidMiserieWinners() {
            Round mockRound = createValidMockRound();
            when(mockRound.getHighestBid()).thenReturn(new MiserieBid(p1.getId(), BidType.MISERIE));
            when(mockRound.isFinished()).thenReturn(true);

            Player alienPlayer = new Player(new HumanStrategy(), "Alien", new PlayerId());
            when(mockRound.getWinningPlayers()).thenReturn(List.of(alienPlayer));

            when(mockGame.getRounds()).thenReturn(List.of(mockRound));
            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));
        }

        @Test
        @DisplayName("Rejects rounds with invalid score deltas (null, or wrong size)")
        void shouldThrowOnInvalidDeltas() {
            Round mockRound = createValidMockRound();
            when(mockRound.getScoreDeltas()).thenReturn(List.of(90, -30, -30));
            when(mockRound.getHighestBid()).thenReturn(new SoloBid(p1.getId(), BidType.SOLO, null));
            when(mockRound.isFinished()).thenReturn(true);

            Player alienPlayer = new Player(new HumanStrategy(), "Alien", new PlayerId());
            when(mockRound.getWinningPlayers()).thenReturn(List.of(alienPlayer));

            when(mockGame.getRounds()).thenReturn(List.of(mockRound));
            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));

            when(mockRound.getScoreDeltas()).thenReturn(null);
            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));
        }
        @Test
        @DisplayName("Coverage test - resolveTricksWon() should throw when PASS bid and tricksWon is not -1")
        void shouldCoverageResolveTricksWon() {
            Round mockRound = createValidMockRound();
            when(mockRound.getHighestBid()).thenReturn(new PassBid(p1.getId()));
            when(mockRound.getCountTricksWon()).thenReturn(-1);
            when(mockRound.getBiddingTeamTricksWon()).thenReturn(14);

            when(mockGame.getRounds()).thenReturn(List.of(mockRound));
            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));

            when(mockRound.getHighestBid()).thenReturn(new SoloBid(p1.getId(),  BidType.SOLO, null));
            when(mockRound.getBiddingTeamTricksWon()).thenReturn(-14);
            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));
            when(mockRound.getHighestBid()).thenReturn(new SoloBid(p1.getId(),  BidType.SOLO, null));
            when(mockRound.getBiddingTeamTricksWon()).thenReturn(14);
            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));
        }

        @Test
        @DisplayName("Rejects non-PASS rounds with zero participants")
        void shouldThrowOnZeroParticipantsForNormalBid() {
            Round mockRound = createValidMockRound();
            when(mockRound.getHighestBid()).thenReturn(new SoloBid(p1.getId(), BidType.SOLO, Suit.HEARTS));
            when(mockRound.getBiddingTeamPlayers()).thenReturn(List.of()); // Empty participants!

            when(mockGame.getRounds()).thenReturn(List.of(mockRound));
            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));
        }
    }

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
            assertEquals(restoredPlayers.get(0), restoredPlayers.get(0));
        }

        @Test
        @DisplayName("Successfully loads a COUNT mode save without restoring the deck")
        void shouldLoadCountModeWithoutDeck() {
            List<RoundSnapshot> rounds = List.of(
                    new RoundSnapshot(BidType.PASS, 0, List.of(0), -1, List.of(), 1, List.of(0, 0, 0, 0), Suit.HEARTS)
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
            List<PlayerSnapshot> playerSnapshots = List.of(new PlayerSnapshot(new PlayerId().id().toString(), "P1", StrategySnapshotType.HUMAN, 0));
            List<RoundSnapshot> rounds = List.of(
                    new RoundSnapshot(BidType.SOLO, 0, List.of(0), 13, List.of(), 1, List.of(90, -30, -30, -30), Suit.HEARTS)
            );
            GameSnapshot mockSnapshot = new GameSnapshot("Desc", SaveMode.COUNT, 0, playerSnapshots, rounds);
            when(mockRepository.loadByDescription("bad_restore")).thenReturn(mockSnapshot);

            List<Player> restoredPlayers = new ArrayList<>();
            when(mockGame.getPlayers()).thenReturn(restoredPlayers); // Mock list will only have 1 player after reset

            assertThrows(IllegalStateException.class, () -> persistenceService.loadIntoGame(mockGame, "bad_restore"));
        }

        @Test
        @DisplayName("Successfully restores all bot strategy types during load")
        void shouldRestoreAllStrategyTypes() {
            // Create a snapshot with exactly one of every strategy type
            List<PlayerSnapshot> variedPlayers = List.of(
                    new PlayerSnapshot(new PlayerId().id().toString(), "P1", StrategySnapshotType.HUMAN, 0),
                    new PlayerSnapshot(new PlayerId().id().toString(), "P2", StrategySnapshotType.HIGH_BOT, 0),
                    new PlayerSnapshot(new PlayerId().id().toString(), "P3", StrategySnapshotType.LOW_BOT, 0),
                    new PlayerSnapshot(new PlayerId().id().toString(), "P4", StrategySnapshotType.SMART_BOT, 0)
            );
            GameSnapshot snapshot = new GameSnapshot("Bot Load", SaveMode.GAME, 0, variedPlayers, List.of());
            when(mockRepository.loadByDescription("Bot Load")).thenReturn(snapshot);

            // Mock the game to accept the restored players
            List<Player> restoredPlayers = new ArrayList<>();
            when(mockGame.getPlayers()).thenReturn(restoredPlayers);
            doAnswer(invocation -> {
                restoredPlayers.add(invocation.getArgument(0));
                return null;
            }).when(mockGame).addPlayer(any(Player.class));

            // Act
            persistenceService.loadIntoGame(mockGame, "Bot Load");

            // Assert: Verify every single strategy was mapped back to the correct Domain Strategy class
            assertEquals(4, restoredPlayers.size());
            assertInstanceOf(HumanStrategy.class, restoredPlayers.get(0).getDecisionStrategy());
            assertInstanceOf(HighBotStrategy.class, restoredPlayers.get(1).getDecisionStrategy());
            assertInstanceOf(LowBotStrategy.class, restoredPlayers.get(2).getDecisionStrategy());
            assertInstanceOf(SmartBotStrategy.class, restoredPlayers.get(3).getDecisionStrategy());
        }
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