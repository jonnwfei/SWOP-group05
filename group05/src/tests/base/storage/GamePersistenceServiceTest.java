package base.storage;

import base.domain.WhistGame;
import base.domain.bid.Bid;
import base.domain.bid.BidType;
import base.domain.bid.SoloBid;
import base.domain.card.Suit;
import base.domain.strategy.*;

import base.domain.player.Player;
import base.domain.round.Round;
import base.storage.snapshots.GameSnapshot;
import base.storage.snapshots.PlayerSnapshot;
import base.storage.snapshots.RoundSnapshot;
import base.storage.snapshots.SaveMode;
import base.storage.snapshots.StrategySnapshotType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GamePersistenceServiceTest {

    private SaveRepository mockRepository;
    private GamePersistenceService persistenceService;
    private WhistGame mockGame;

    // Standard 4-player setup
    private Player p1, p2, p3, p4;
    private List<Player> fourPlayers;

    @BeforeEach
    void setUp() {
        mockRepository = mock(SaveRepository.class);
        persistenceService = new GamePersistenceService(mockRepository);
        mockGame = mock(WhistGame.class);

        p1 = new Player(new HumanStrategy(), "P1");
        p2 = new Player(new HumanStrategy(), "P2");
        p3 = new Player(new HumanStrategy(), "P3");
        p4 = new Player(new HumanStrategy(), "P4");
        fourPlayers = List.of(p1, p2, p3, p4);

        // Default mock behaviors to prevent NullPointerExceptions in basic tests
        when(mockGame.getPlayers()).thenReturn(fourPlayers);
        when(mockGame.getDealerPlayer()).thenReturn(p1);
        when(mockGame.getRounds()).thenReturn(List.of());
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private Round createValidMockRound() {
        Round mockRound = mock(Round.class);
        Bid realBid = new SoloBid(p1, BidType.SOLO, Suit.HEARTS);

        when(mockRound.getHighestBid()).thenReturn(realBid);

        when(mockRound.getPlayers()).thenReturn(fourPlayers);
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
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Rejects null repository")
        void testConstructorDefensive() {
            assertThrows(IllegalArgumentException.class, () -> new GamePersistenceService(null),
                    "Service must not initialize with a null repository.");
        }

        @Test
        @DisplayName("Initializes successfully with default repository")
        void testConstructor() {
            assertDoesNotThrow(() -> new GamePersistenceService());
        }
    }

    // =========================================================================
    // SAVE TESTS (GameSnapshot & PlayerSnapshot Creation)
    // =========================================================================

    @Nested
    @DisplayName("Save Game & Player Data Tests")
    class SaveGameTests {

        @Test
        @DisplayName("Successfully saves a game in GAME mode")
        void testSaveGameMainSuccessScenario() {
            String description = "late night stretch";
            SaveMode mode = SaveMode.GAME;
            p1.updateScore(15);

            persistenceService.save(mockGame, mode, description);

            ArgumentCaptor<GameSnapshot> snapshotCaptor = ArgumentCaptor.forClass(GameSnapshot.class);
            verify(mockRepository, times(1)).save(snapshotCaptor.capture());
            GameSnapshot savedSnapshot = snapshotCaptor.getValue();

            assertAll(
                    () -> assertEquals(description, savedSnapshot.description()),
                    () -> assertEquals(mode, savedSnapshot.mode()),
                    () -> assertEquals(0, savedSnapshot.dealerIndex()),
                    () -> assertEquals(4, savedSnapshot.players().size()),
                    () -> assertEquals("P1", savedSnapshot.players().get(0).name()),
                    () -> assertEquals(15, savedSnapshot.players().get(0).score())
            );
        }

        @Test
        @DisplayName("Successfully maps strategy types (HighBot, LowBot)")
        void testSaveGameStrategyMapping() {
            Player highBot = new Player(new HighBotStrategy(), "HighBot");
            Player lowBot = new Player(new LowBotStrategy(), "LowBot");
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
        @DisplayName("Rejects null or blank arguments")
        void testSaveGameDefensive() {
            assertAll("Defensive save constraints",
                    () -> assertThrows(IllegalArgumentException.class, () -> persistenceService.save(null, SaveMode.GAME, "Desc")),
                    () -> assertThrows(IllegalArgumentException.class, () -> persistenceService.save(mockGame, null, "Desc")),
                    () -> assertThrows(IllegalArgumentException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, null)),
                    () -> assertThrows(IllegalArgumentException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "   "))
            );
        }

        @Test
        @DisplayName("Rejects saving if dealer is null")
        void testSaveGameNullDealerThrows() {
            when(mockGame.getDealerPlayer()).thenReturn(null);
            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Null Dealer Save"));
        }

        @Test
        @DisplayName("Rejects saving if dealer is not in the players list")
        void testSaveGameDealerNotInPlayersThrows() {
            Player ghostDealer = new Player(new HumanStrategy(), "Ghost");
            when(mockGame.getDealerPlayer()).thenReturn(ghostDealer);
            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Ghost Save"));
        }

        @Test
        @DisplayName("Rejects saving if players list contains a null player")
        void testSaveGameNullPlayer() {
            List<Player> invalidPlayers = new ArrayList<>();
            invalidPlayers.add(p1);
            invalidPlayers.add(null);
            when(mockGame.getPlayers()).thenReturn(invalidPlayers);

            assertThrows(IllegalArgumentException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));
        }

        @Test
        @DisplayName("Rejects saving if a player has a null strategy")
        void testSaveGameNullStrategy() {
            Player badPlayer = mock(Player.class);
            when(badPlayer.getName()).thenReturn("Bad");
            when(badPlayer.getDecisionStrategy()).thenReturn(null);

            when(mockGame.getPlayers()).thenReturn(List.of(badPlayer, p2, p3, p4));
            assertThrows(IllegalArgumentException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));
        }
    }

    // =========================================================================
    // ROUND SNAPSHOT (SERIALIZATION) TESTS
    // =========================================================================

    @Nested
    @DisplayName("Round Serialization Tests (toSnapshot)")
    class RoundSerializationTests {

        @Test
        @DisplayName("Successfully serializes a valid round")
        void testSaveGameWithValidRound() {
            Round validRound = createValidMockRound();
            when(mockGame.getRounds()).thenReturn(List.of(validRound));

            assertDoesNotThrow(() -> persistenceService.save(mockGame, SaveMode.GAME, "Round Save"));
        }

        @Test
        @DisplayName("Rejects null round elements")
        void testSaveGameWithNullRoundElements() {
            when(mockGame.getRounds()).thenReturn(Collections.singletonList(null));
            assertThrows(IllegalArgumentException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));

            Round mockRound = createValidMockRound();
            when(mockRound.getHighestBid()).thenReturn(null);
            when(mockGame.getRounds()).thenReturn(List.of(mockRound));
            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));
        }

        @Test
        @DisplayName("Rejects round with invalid player mappings")
        void testSaveGameWithInvalidRoundPlayerMappings() {
            Round mockRound = createValidMockRound();
            when(mockGame.getRounds()).thenReturn(List.of(mockRound));

            when(mockRound.getPlayers()).thenReturn(List.of(p1, p2, p3));
            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));

            when(mockRound.getPlayers()).thenReturn(fourPlayers);
            Player ghostPlayer = new Player(new HumanStrategy(), "Ghost");
            Bid badBid = new SoloBid(ghostPlayer, BidType.SOLO, Suit.HEARTS);
            when(mockRound.getHighestBid()).thenReturn(badBid);
            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));
        }

        @Test
        @DisplayName("Rejects invalid participant or miserie winner mappings")
        void testSaveGameWithInvalidParticipants() {
            Round mockRound = createValidMockRound();
            when(mockGame.getRounds()).thenReturn(List.of(mockRound));

            when(mockRound.getBiddingTeamPlayers()).thenReturn(List.of());
            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));

            when(mockRound.getBiddingTeamPlayers()).thenReturn(List.of(new Player(new HumanStrategy(), "Ghost")));
            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));

            when(mockRound.getBiddingTeamPlayers()).thenReturn(List.of(p1));
            when(mockRound.getCountMiserieWinners()).thenReturn(List.of(new Player(new HumanStrategy(), "Ghost")));
            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));
        }

        @Test
        @DisplayName("Rejects invalid deltas or tricks won")
        void testSaveGameWithInvalidDeltasOrTricks() {
            Round mockRound = createValidMockRound();
            when(mockGame.getRounds()).thenReturn(List.of(mockRound));

            when(mockRound.getScoreDeltas()).thenReturn(List.of(0, 0));
            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));

            when(mockRound.getScoreDeltas()).thenReturn(List.of(90, -30, -30, -30));
            when(mockRound.getCountTricksWon()).thenReturn(-5);
            when(mockRound.getBiddingTeamTricksWon()).thenReturn(-2);
            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));

            when(mockRound.getCountTricksWon()).thenReturn(-1);
            when(mockRound.getBiddingTeamTricksWon()).thenReturn(14);
            assertThrows(IllegalStateException.class, () -> persistenceService.save(mockGame, SaveMode.GAME, "Test"));
        }
    }

    // =========================================================================
    // LOAD TESTS (Restoring Players and Rounds)
    // =========================================================================

    @Nested
    @DisplayName("Load Game & Restore History Tests")
    class LoadGameTests {

        @Test
        @DisplayName("Successfully loads a GAME mode save and restores dealer")
        void testLoadIntoGameMainSuccessScenario() {
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
            verify(mockGame).setDealerPlayer(restoredPlayers.get(0));
        }

        @Test
        @DisplayName("Successfully loads a COUNT mode save without setting a deck")
        void testLoadCountMode() {
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
            verify(mockGame).setDealerPlayer(restoredPlayers.get(0));
        }

        @Test
        @DisplayName("Rejects loading with null arguments")
        void testLoadIntoGameDefensive() {
            when(mockRepository.loadByDescription("_")).thenThrow(new IllegalArgumentException("No save found with description: _"));
            assertAll("Defensive load constraints",
                    () -> assertThrows(IllegalArgumentException.class, () -> persistenceService.loadIntoGame(null, "Desc")),
                    () -> assertThrows(IllegalArgumentException.class, () -> persistenceService.loadIntoGame(mockGame, null)),
                    () -> assertThrows(IllegalArgumentException.class, () -> persistenceService.loadIntoGame(mockGame, "_"))
            );
        }

        @Test
        @DisplayName("Restoring rounds to a game without exactly 4 players throws IllegalStateException")
        void testRestoreRoundHistoryNot4Players() {
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

            assertThrows(IllegalStateException.class, () -> persistenceService.loadIntoGame(mockGame, "bad_restore"));
        }

        @Test
        @DisplayName("Successfully restores valid round history")
        void testRestoreValidRounds() {
            List<RoundSnapshot> rounds = List.of(
                    new RoundSnapshot(BidType.SOLO, 2, List.of(2), 13, List.of(), 1, List.of(-30, -30, 90, -30))
            );
            GameSnapshot snapshot = new GameSnapshot("Desc", SaveMode.COUNT, 0, createValidPlayerSnapshots(), rounds);
            when(mockRepository.loadByDescription("Test")).thenReturn(snapshot);

            List<Player> restoredPlayers = new ArrayList<>();
            when(mockGame.getPlayers()).thenReturn(restoredPlayers);
            doAnswer(invocation -> {
                restoredPlayers.add(invocation.getArgument(0));
                return null;
            }).when(mockGame).addPlayer(any(Player.class));

            persistenceService.loadIntoGame(mockGame, "Test");

            verify(mockGame, times(1)).addRound(any(Round.class));
        }
    }

    // =========================================================================
    // REPOSITORY DELEGATION TESTS
    // =========================================================================

    @Nested
    @DisplayName("Repository Delegation Tests")
    class RepositoryDelegationTests {

        @Test
        @DisplayName("Delegates listDescriptions properly")
        void testListDescriptions() {
            List<String> expectedList = List.of("Save 1", "Save 2");
            when(mockRepository.listDescriptions()).thenReturn(expectedList);

            List<String> actualList = persistenceService.listDescriptions();

            assertEquals(expectedList, actualList);
            verify(mockRepository, times(1)).listDescriptions();
        }
    }
}