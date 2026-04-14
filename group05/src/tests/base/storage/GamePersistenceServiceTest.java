package base.storage;

import base.domain.WhistGame;

import base.domain.player.Player;
import base.domain.snapshots.GameSnapshot;
import base.domain.snapshots.PlayerSnapshot;
import base.domain.snapshots.SaveMode;
import base.domain.snapshots.StrategySnapshotType;
import base.domain.strategy.HighBotStrategy;
import base.domain.strategy.HumanStrategy;
import base.domain.strategy.LowBotStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GamePersistenceServiceTest {

    private SaveRepository mockRepository;
    private GamePersistenceService persistenceService;
    private WhistGame mockGame;

    @BeforeEach
    void setUp() {
        mockRepository = mock(SaveRepository.class);
        persistenceService = new GamePersistenceService(mockRepository);
        mockGame = mock(WhistGame.class);
    }

    @Test
    @DisplayName("Constructor handles illegal arguments defensively")
    void testConstructorDefensive() {
        assertThrows(IllegalArgumentException.class, () -> new GamePersistenceService(null),
                "Service must not initialize with a null repository.");
    }

    @Test
    @DisplayName("Constructor initializes with a valid repository or defaults")
    void testConstructor() {
        new GamePersistenceService(); // should not throw
    }

    @Test
    @DisplayName("Use Case 4.4: Save game/count")
    void testSaveGameMainSuccessScenario() {
        String description = "late night stretch";
        SaveMode mode = SaveMode.GAME;

        Player mockPlayer = new Player(new HumanStrategy(), "Tommy");
        mockPlayer.updateScore(15);

        when(mockGame.getPlayers()).thenReturn(List.of(mockPlayer));
        when(mockGame.getDealerPlayer()).thenReturn(mockPlayer);

        persistenceService.save(mockGame, mode, description);

        ArgumentCaptor<GameSnapshot> snapshotCaptor = ArgumentCaptor.forClass(GameSnapshot.class);
        verify(mockRepository, times(1)).save(snapshotCaptor.capture());

        GameSnapshot savedSnapshot = snapshotCaptor.getValue();

        assertEquals(description, savedSnapshot.description(), "Description should match.");
        assertEquals(mode, savedSnapshot.mode(), "Save mode should match.");
        assertEquals(0, savedSnapshot.dealerIndex(), "Dealer index should correspond to Tommy's position.");
        assertEquals(1, savedSnapshot.players().size(), "Should contain 1 player snapshot.");
        assertEquals("Tommy", savedSnapshot.players().get(0).name(), "Player name should be saved.");
        assertEquals(15, savedSnapshot.players().get(0).score(), "Player score should be saved.");
    }

    @Test
    @DisplayName("Use Case 4.4: Save game/count, Defensiveness")
    void testSaveGameDefensive() {
        assertAll("Defensive save constraints",
                () -> assertThrows(IllegalArgumentException.class,
                        () -> persistenceService.save(null, SaveMode.GAME, "Desc")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> persistenceService.save(mockGame, null, "Desc")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> persistenceService.save(mockGame, SaveMode.GAME, null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> persistenceService.save(mockGame, SaveMode.GAME, "   ")) // Blank description check triggered by createSnapshot
        );
    }

    @Test
    @DisplayName("Save game covers HighBot, LowBot, and a null dealer index")
    void testSaveGameOtherSituations() {
        Player highBot = new Player(new HighBotStrategy(), "HighBot");
        Player lowBot = new Player(new LowBotStrategy(), "LowBot"); // TODO: add SmartBot Later

        when(mockGame.getPlayers()).thenReturn(List.of(highBot, lowBot));

        when(mockGame.getDealerPlayer()).thenReturn(highBot);

        persistenceService.save(mockGame, SaveMode.GAME, "Bot Save");

        ArgumentCaptor<GameSnapshot> captor = ArgumentCaptor.forClass(GameSnapshot.class);
        verify(mockRepository).save(captor.capture());

        GameSnapshot saved = captor.getValue();
        assertEquals(0,saved.dealerIndex(), "Dealer index should be 0 for HighBot");
        assertEquals(StrategySnapshotType.HIGH_BOT, saved.players().get(0).strategyType());
        assertEquals(StrategySnapshotType.LOW_BOT, saved.players().get(1).strategyType());
    }

    @Test
    @DisplayName("save Dealer is Null, throws IllegalState")
    void testSaveGameNullDealerThrows() {
        Player mockPlayer = new Player(new HumanStrategy(), "Tommy");
        when(mockGame.getPlayers()).thenReturn(List.of(mockPlayer));
        when(mockGame.getDealerPlayer()).thenReturn(null);

        assertThrows(IllegalStateException.class,
                () -> persistenceService.save(mockGame, SaveMode.GAME, "Null Dealer Save"),
                "System should reject saving if the dealer is missing, as it breaks future player rotation."
        );
    }

    @Test
    @DisplayName("save whenDealer not in player list, throws IllegalState")
    void testSaveGameDealerNotInPlayersThrows() {
        Player activePlayer = new Player(new HumanStrategy(), "Alice");
        Player ghostDealer = new Player(new HumanStrategy(), "Ghost");

        when(mockGame.getPlayers()).thenReturn(List.of(activePlayer));
        when(mockGame.getDealerPlayer()).thenReturn(ghostDealer);


        assertThrows(IllegalStateException.class,
                () -> persistenceService.save(mockGame, SaveMode.GAME, "Ghost Save"),
                "System should reject saving if the dealer isn't a registered player."
        );
    }

    @Test
    @DisplayName("Use Case 4.5: Resume game/count - Main Success Scenario (Game Mode)")
    void testLoadIntoGameMainSuccessScenario() {
        String description = "Saved Game";
        List<PlayerSnapshot> playerSnapshots = List.of(
                new PlayerSnapshot("Stan", StrategySnapshotType.HUMAN, 10)
        );
        GameSnapshot mockSnapshot = new GameSnapshot(description, SaveMode.GAME, 0, playerSnapshots);
        when(mockRepository.loadByDescription(description)).thenReturn(mockSnapshot);

        // Backing list to emulate WhistGame internal state
        List<Player> restoredPlayers = new java.util.ArrayList<>();
        when(mockGame.getPlayers()).thenReturn(restoredPlayers);

        // Simulate addPlayer mutating game state
        doAnswer(invocation -> {
            Player p = invocation.getArgument(0);
            restoredPlayers.add(p);
            return null;
        }).when(mockGame).addPlayer(any(Player.class));

        SaveMode resultMode = persistenceService.loadIntoGame(mockGame, description);

        assertEquals(SaveMode.GAME, resultMode);
        verify(mockGame, times(1)).resetPlayers();
        verify(mockGame, times(1)).resetRounds();
        verify(mockGame, times(1)).addPlayer(any(Player.class));
        verify(mockGame, times(1)).setDeck(any());
        verify(mockGame, times(1)).setDealerPlayer(restoredPlayers.get(0));
    }


    @Test
    @DisplayName("Use Case 4.5: Resume game/count - Negative and Edge Cases")
    void testLoadIntoGameDefensive() {
        assertAll("Defensive load constraints",
                () -> assertThrows(IllegalArgumentException.class,
                        () -> persistenceService.loadIntoGame(null, "Desc")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> persistenceService.loadIntoGame(mockGame, null))
        );

        when(mockRepository.loadByDescription("Unknown")).thenReturn(null);
        SaveMode mode = persistenceService.loadIntoGame(mockGame, "Unknown");
        assertNull(mode, "Loading a non-existent description should return null.");
    }

    @Test
    @DisplayName("Private mapping methods handle nulls inside lists defensively")
    void testInternalMappingDefensive() {
        // Test null player in the list
        when(mockGame.getPlayers()).thenReturn(Collections.singletonList(null));
        assertThrows(IllegalArgumentException.class,
                () -> persistenceService.save(mockGame, SaveMode.GAME, "Test")
        );

        // Test player with null strategy
        Player badPlayer = mock(Player.class);
        when(badPlayer.getName()).thenReturn("Bad");
        when(badPlayer.getDecisionStrategy()).thenReturn(null); // Triggers toStrategyType(null)

        when(mockGame.getPlayers()).thenReturn(List.of(badPlayer));
        assertThrows(IllegalArgumentException.class,
                () -> persistenceService.save(mockGame, SaveMode.GAME, "Test")
        );
    }

    @Test
    @DisplayName("Use Case 4.5: Resume COUNT mode covers specific restore branches")
    void testLoadCountMode() {
        List<PlayerSnapshot> playerSnapshots = List.of(
                new PlayerSnapshot("HighBot", StrategySnapshotType.HIGH_BOT, 5),
                new PlayerSnapshot("LowBot", StrategySnapshotType.LOW_BOT, -5)
        );
        // snapshot with dealerIndex 0
        GameSnapshot countSnapshot = new GameSnapshot("Count Save", SaveMode.COUNT, 0, playerSnapshots);

        when(mockRepository.loadByDescription("Count Save")).thenReturn(countSnapshot);

        List<Player> restoredPlayers = new java.util.ArrayList<>();
        when(mockGame.getPlayers()).thenReturn(restoredPlayers);
        doAnswer(invocation -> {
            Player p = invocation.getArgument(0);
            restoredPlayers.add(p);
            return null;
        }).when(mockGame).addPlayer(any(Player.class));

        SaveMode mode = persistenceService.loadIntoGame(mockGame, "Count Save");

        assertEquals(SaveMode.COUNT, mode);
        verify(mockGame, never()).setDeck(any()); // Deck should not be set for COUNT

        verify(mockGame, times(1)).setDealerPlayer(restoredPlayers.get(0));
    }

    @Test
    @DisplayName("List descriptions delegates properly")
    void testListDescriptions() {
        List<String> expectedList = List.of("Save 1", "Save 2");
        when(mockRepository.listDescriptions()).thenReturn(expectedList);

        List<String> actualList = persistenceService.listDescriptions();

        assertEquals(expectedList, actualList, "Should return the exact list from the repository.");
        verify(mockRepository, times(1)).listDescriptions();
    }

    @Test
    @DisplayName("Load GAME mode successfully sets a valid dealer")
    void testLoadGameSetsValidDealer() {
        List<PlayerSnapshot> playerSnapshots = List.of(
                new PlayerSnapshot("Tommy", StrategySnapshotType.HUMAN, 0)
        );
        GameSnapshot gameSnapshot = new GameSnapshot("Valid Dealer Save", SaveMode.GAME, 0, playerSnapshots);
        when(mockRepository.loadByDescription("Valid Dealer Save")).thenReturn(gameSnapshot);

        // Use the backing list approach for consistency!
        List<Player> restoredPlayers = new java.util.ArrayList<>();
        when(mockGame.getPlayers()).thenReturn(restoredPlayers);

        doAnswer(invocation -> {
            Player p = invocation.getArgument(0);
            restoredPlayers.add(p);
            return null;
        }).when(mockGame).addPlayer(any(Player.class));

        persistenceService.loadIntoGame(mockGame, "Valid Dealer Save");

        // Verify the exact dealer was set based on the index
        verify(mockGame).setDealerPlayer(restoredPlayers.get(0));
    }
}