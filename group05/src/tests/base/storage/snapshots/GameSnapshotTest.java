package base.storage.snapshots;

import base.domain.player.PlayerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class GameSnapshotTest {
    private List<PlayerSnapshot> playerSnapshots;
    private final String GAME_DESC = "Test Round 1";

    @BeforeEach
    void setUp() {
        playerSnapshots = List.of(
                new PlayerSnapshot(new PlayerId().id().toString(),"Stan", StrategySnapshotType.HUMAN, 10),
                new PlayerSnapshot(new PlayerId().id().toString(),"Seppe", StrategySnapshotType.LOW_BOT, -5), // TODO: change to SMART_BOT
                new PlayerSnapshot(new PlayerId().id().toString(),"Tommy", StrategySnapshotType.LOW_BOT, -5),
                new PlayerSnapshot(new PlayerId().id().toString(),"John", StrategySnapshotType.HIGH_BOT, 0));
    }

    @Test
    @DisplayName("Verify full state restoration for Use Case 4.5")
    void testFullStateIntegrity() {
        GameSnapshot snapshot = new GameSnapshot(GAME_DESC, SaveMode.GAME, 1, playerSnapshots, List.of());

        assertAll("Snapshot must preserve all domain state",
                () -> assertEquals(GAME_DESC, snapshot.description(), "Description mismatch"),
                () -> assertEquals(SaveMode.GAME, snapshot.mode(), "Save mode (Game vs Count) mismatch"),
                () -> assertEquals(1, snapshot.dealerIndex(), "Dealer rotation index mismatch"),
                () -> assertEquals(4, snapshot.players().size(), "Player count mismatch"),
                () -> assertEquals("Stan", snapshot.players().get(0).name(), "Player data corruption"));
    }

    @Test
    @DisplayName("Negative Scenario: Defensive check for null players")
    void testDefensiveConstructor() {
        assertThrows(IllegalArgumentException.class,
                () -> new GameSnapshot("Invalid", SaveMode.COUNT, 0, null, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new GameSnapshot("Invalid", null, 0, playerSnapshots, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new GameSnapshot(null, SaveMode.COUNT, 0, playerSnapshots, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new GameSnapshot("Invalid", SaveMode.COUNT, null, playerSnapshots, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new GameSnapshot("Invalid", SaveMode.COUNT, -1, playerSnapshots, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new GameSnapshot("Invalid", SaveMode.COUNT,
                playerSnapshots.size() + 1, playerSnapshots, List.of()));
        List<PlayerSnapshot> playerSnapshots2 = new ArrayList<>(playerSnapshots);
        playerSnapshots2.removeFirst();
        playerSnapshots2.add(null);
        assertThrows(IllegalArgumentException.class,
                () -> new GameSnapshot("Invalid", SaveMode.COUNT, 0, playerSnapshots2, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new GameSnapshot("Invalid", SaveMode.COUNT, 0, playerSnapshots, null));

    }

    @Test
    @DisplayName("Verify immutability of the player list")
    void testImmutability() {
        List<PlayerSnapshot> modifiableList = new ArrayList<>(playerSnapshots);
        List<RoundSnapshot> modifiableRoundList = new ArrayList<>();
        GameSnapshot snapshot = new GameSnapshot(GAME_DESC, SaveMode.GAME, 0, modifiableList, modifiableRoundList);

        modifiableList.clear();
        modifiableRoundList.add(
                new RoundSnapshot(base.domain.bid.BidType.PASS, 0, List.of(0), -1, List.of(), 1, List.of(0, 0, 0, 0)));

        assertFalse(snapshot.players().isEmpty(), "Snapshot should be immune to external list mutations.");
        assertTrue(snapshot.rounds().isEmpty(), "Round snapshots should be immune to external list mutations.");
    }
}