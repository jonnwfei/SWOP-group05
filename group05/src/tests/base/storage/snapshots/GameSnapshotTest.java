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
                new PlayerSnapshot(new PlayerId().id().toString(),"Seppe", StrategySnapshotType.SMART_BOT, -5), // FIXED TODO
                new PlayerSnapshot(new PlayerId().id().toString(),"Tommy", StrategySnapshotType.LOW_BOT, -5),
                new PlayerSnapshot(new PlayerId().id().toString(),"John", StrategySnapshotType.HIGH_BOT, 0));
    }

    @Test
    @DisplayName("Verify full state restoration for Use Case 4.5")
    void testFullStateIntegrity() {
        RoundSnapshot roundSnapshot = new RoundSnapshot(base.domain.bid.BidType.PASS, 0, List.of(0), -1, List.of(), 1, List.of(0, 0, 0, 0));
        GameSnapshot snapshot = new GameSnapshot(GAME_DESC, SaveMode.GAME, 1, playerSnapshots, List.of(roundSnapshot));

        assertAll("Snapshot must preserve all domain state",
                () -> assertEquals(GAME_DESC, snapshot.description(), "Description mismatch"),
                () -> assertEquals(SaveMode.GAME, snapshot.mode(), "Save mode (Game vs Count) mismatch"),
                () -> assertEquals(1, snapshot.dealerIndex(), "Dealer rotation index mismatch"),
                () -> assertEquals(4, snapshot.players().size(), "Player count mismatch"),
                () -> assertEquals("Stan", snapshot.players().getFirst().name(), "Player data corruption"));
    }

    @Test
    @DisplayName("Defensive Constructor Tests - should throw IllegalArgumentException for invalid inputs")
    void testDefensiveConstructor() {
        // Null primitive parameters
        assertThrows(IllegalArgumentException.class,
                () -> new GameSnapshot(null, SaveMode.COUNT, 0, playerSnapshots, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new GameSnapshot("Invalid", null, 0, playerSnapshots, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new GameSnapshot("Invalid", SaveMode.COUNT, null, playerSnapshots, List.of()));

        // Null list parameters
        assertThrows(IllegalArgumentException.class,
                () -> new GameSnapshot("Invalid", SaveMode.COUNT, 0, null, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new GameSnapshot("Invalid", SaveMode.COUNT, 0, playerSnapshots, null));

        // Invalid Dealer Indices
        assertThrows(IllegalArgumentException.class,
                () -> new GameSnapshot("Invalid", SaveMode.COUNT, -1, playerSnapshots, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new GameSnapshot("Invalid", SaveMode.COUNT, playerSnapshots.size(), playerSnapshots, List.of()));

        // List containing null player
        List<PlayerSnapshot> playerSnapshotsWithNull = new ArrayList<>(playerSnapshots);
        playerSnapshotsWithNull.removeFirst();
        playerSnapshotsWithNull.add(null);
        assertThrows(IllegalArgumentException.class,
                () -> new GameSnapshot("Invalid", SaveMode.COUNT, 0, playerSnapshotsWithNull, List.of()));

        // List containing null round (This was missing to get 100% coverage!)
        List<RoundSnapshot> roundsWithNull = new ArrayList<>();
        roundsWithNull.add(null);
        assertThrows(IllegalArgumentException.class,
                () -> new GameSnapshot("Invalid", SaveMode.COUNT, 0, playerSnapshots, roundsWithNull));
    }

    @Test
    @DisplayName("Verify immutability of the player and round lists")
    void testImmutability() {
        List<PlayerSnapshot> modifiableList = new ArrayList<>(playerSnapshots);
        List<RoundSnapshot> modifiableRoundList = new ArrayList<>();
        GameSnapshot snapshot = new GameSnapshot(GAME_DESC, SaveMode.GAME, 0, modifiableList, modifiableRoundList);

        // 1. External Mutation Test
        modifiableList.clear();
        modifiableRoundList.add(
                new RoundSnapshot(base.domain.bid.BidType.PASS, 0, List.of(0), -1, List.of(), 1, List.of(0, 0, 0, 0)));

        assertFalse(snapshot.players().isEmpty(), "Snapshot should be immune to external player list mutations.");
        assertTrue(snapshot.rounds().isEmpty(), "Snapshot should be immune to external round list mutations.");

        // 2. Internal Mutation Test (Proving List.copyOf worked)
        assertThrows(UnsupportedOperationException.class, () -> snapshot.players().clear(),
                "Should not be able to clear the players list from the getter");
        assertThrows(UnsupportedOperationException.class, () -> snapshot.rounds().add(null),
                "Should not be able to add to the rounds list from the getter");
    }
}