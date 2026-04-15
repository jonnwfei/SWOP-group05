package base.storage.snapshots;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerSnapshotTest {

    @Test
    void testPlayerSnapshot() {
        String expectedName = "Player1";
        StrategySnapshotType expectedType = StrategySnapshotType.LOW_BOT;
        int expectedScore = 15;

        PlayerSnapshot snapshot = new PlayerSnapshot(expectedName, expectedType, expectedScore);

        assertEquals(expectedName, snapshot.name(), "Name should match the constructor input.");
        assertEquals(expectedType, snapshot.strategyType(), "Strategy type should match the constructor input.");
        assertEquals(expectedScore, snapshot.score(), "Score should match the constructor input.");
    }

    @Test
    void testDefensiveConstructor() {
        assertThrows(IllegalArgumentException.class, () ->
                new PlayerSnapshot("testName", null, 0)
        );
        assertThrows(IllegalArgumentException.class, () ->
                new PlayerSnapshot(null, StrategySnapshotType.HUMAN, 0)
        );
        assertThrows(IllegalArgumentException.class, () ->
                new PlayerSnapshot("    ", StrategySnapshotType.HUMAN, 0)
        );
    }

    @Test
    void testEquality() {
        PlayerSnapshot snapshot1 = new PlayerSnapshot("AI_Bot", StrategySnapshotType.LOW_BOT, 10);
        PlayerSnapshot snapshot2 = new PlayerSnapshot("AI_Bot", StrategySnapshotType.LOW_BOT, 10);

        PlayerSnapshot snapshot3 = new PlayerSnapshot("Human", StrategySnapshotType.HUMAN, 10);

        assertEquals(snapshot1, snapshot2, "Snapshots with identical data must be equal.");
        assertEquals(snapshot1.hashCode(), snapshot2.hashCode(), "Identical snapshots must have the same hash code.");
        assertNotEquals(snapshot1, snapshot3, "Snapshots with different names or types must not be equal.");
    }

    @Test
    void testToString() {
        PlayerSnapshot snapshot = new PlayerSnapshot("Alice", StrategySnapshotType.HUMAN, 5);
        String result = snapshot.toString();

        assertTrue(result.contains("Alice"));
        assertTrue(result.contains("HUMAN"));
        assertTrue(result.contains("5"));
    }
}