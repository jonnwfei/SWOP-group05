package base.storage.snapshots;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Player Snapshot Tests")
class PlayerSnapshotTest {

    @Test
    @DisplayName("Should successfully create a PlayerSnapshot with valid fields")
    void testPlayerSnapshot() {
        String expectedId = UUID.randomUUID().toString();
        String expectedName = "Player1";
        StrategySnapshotType expectedType = StrategySnapshotType.LOW_BOT;
        int expectedScore = 15;

        PlayerSnapshot snapshot = new PlayerSnapshot(expectedId, expectedName, expectedType, expectedScore);

        assertEquals(expectedId, snapshot.id(), "ID should match the constructor input.");
        assertEquals(expectedName, snapshot.name(), "Name should match the constructor input.");
        assertEquals(expectedType, snapshot.strategyType(), "Strategy type should match the constructor input.");
        assertEquals(expectedScore, snapshot.score(), "Score should match the constructor input.");
    }

    @Test
    @DisplayName("Defensive constructor should reject nulls or blank strings")
    void testDefensiveConstructor() {
        String validId = "id-123";
        String validName = "testName";

        // Test invalid ID
        assertThrows(IllegalArgumentException.class, () ->
                new PlayerSnapshot(null, validName, StrategySnapshotType.HUMAN, 0)
        );
        assertThrows(IllegalArgumentException.class, () ->
                new PlayerSnapshot("    ", validName, StrategySnapshotType.HUMAN, 0)
        );

        // Test invalid Name
        assertThrows(IllegalArgumentException.class, () ->
                new PlayerSnapshot(validId, null, StrategySnapshotType.HUMAN, 0)
        );
        assertThrows(IllegalArgumentException.class, () ->
                new PlayerSnapshot(validId, "    ", StrategySnapshotType.HUMAN, 0)
        );

        // Test invalid StrategyType
        assertThrows(IllegalArgumentException.class, () ->
                new PlayerSnapshot(validId, validName, null, 0)
        );
    }

    @Test
    @DisplayName("Equality and HashCode should evaluate based on all fields including ID")
    void testEquality() {
        String sharedId = "shared-uuid-123";
        String diffId = "different-uuid-456";

        PlayerSnapshot snapshot1 = new PlayerSnapshot(sharedId, "AI_Bot", StrategySnapshotType.LOW_BOT, 10);
        PlayerSnapshot snapshot2 = new PlayerSnapshot(sharedId, "AI_Bot", StrategySnapshotType.LOW_BOT, 10);

        PlayerSnapshot snapshot3 = new PlayerSnapshot(diffId, "Human", StrategySnapshotType.HUMAN, 10);
        PlayerSnapshot snapshot4 = new PlayerSnapshot(diffId, "AI_Bot", StrategySnapshotType.LOW_BOT, 10); // Same data, different ID

        // Assert equality for identical data
        assertEquals(snapshot1, snapshot2, "Snapshots with identical data and IDs must be equal.");
        assertEquals(snapshot1.hashCode(), snapshot2.hashCode(), "Identical snapshots must have the same hash code.");

        // Assert inequality
        assertNotEquals(snapshot1, snapshot3, "Snapshots with completely different data must not be equal.");
        assertNotEquals(snapshot1, snapshot4, "Snapshots with identical data but different IDs must not be equal.");
    }

    @Test
    @DisplayName("toString should contain all essential fields including ID")
    void testToString() {
        String testId = "test-uuid-999";
        PlayerSnapshot snapshot = new PlayerSnapshot(testId, "Alice", StrategySnapshotType.HUMAN, 5);

        String result = snapshot.toString();

        assertTrue(result.contains(testId));
        assertTrue(result.contains("Alice"));
        assertTrue(result.contains("HUMAN"));
        assertTrue(result.contains("5"));
    }
}