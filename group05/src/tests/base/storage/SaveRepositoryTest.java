package base.storage;

import base.domain.bid.BidType;
import base.domain.card.Suit;
import base.storage.snapshots.GameSnapshot;
import base.storage.snapshots.PlayerSnapshot;
import base.storage.snapshots.RoundSnapshot;
import base.storage.snapshots.SaveMode;
import base.storage.snapshots.StrategySnapshotType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

@DisplayName("SaveRepository File I/O & Serialization Tests")
class SaveRepositoryTest {

    @TempDir
    Path tempSaveDirectory;

    private SaveRepository saveRepository;
    private GameSnapshot testSnapshot;
    private List<PlayerSnapshot> players;
    private List<RoundSnapshot> rounds;

    @BeforeEach
    void setUp() {
        saveRepository = new SaveRepository(tempSaveDirectory);

        // Extracting raw UUID string correctly to avoid the PlayerId[id=...] record toString() format
        players = List.of(
                new PlayerSnapshot(UUID.randomUUID().toString(), "Tommy", StrategySnapshotType.HUMAN, 10),
                new PlayerSnapshot(UUID.randomUUID().toString(), "Seppe", StrategySnapshotType.HIGH_BOT, -10)
        );

        rounds = List.of(
                new RoundSnapshot(BidType.PASS, 0, List.of(0), -1, List.of(), 1, List.of(0, 0, 0, 0), Suit.HEARTS)
        );

        testSnapshot = new GameSnapshot("Friday Night Game", SaveMode.GAME, 0, players, rounds);
    }

    @Nested
    @DisplayName("Initialization & Constructors")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor handles illegal arguments defensively")
        void testConstructorDefensive() {
            assertThrows(IllegalArgumentException.class, () -> new SaveRepository(null),
                    "Initializing with a null Path should throw an exception.");
        }

        @Test
        @DisplayName("Default constructor initializes safely without throwing")
        void testDefaultConstructor() {
            assertDoesNotThrow(() -> new SaveRepository(),
                    "Default constructor should fall back to a default directory safely.");
        }
    }

    @Nested
    @DisplayName("Save and Load Core Operations")
    class SaveLoadTests {

        @Test
        @DisplayName("Should successfully save and accurately reload a GameSnapshot")
        void testSaveAndLoadSuccess() {
            // Act
            saveRepository.save(testSnapshot);
            GameSnapshot loadedSnapshot = saveRepository.loadByDescription("Friday Night Game");

            // Assert File Creation
            assertTrue(Files.exists(tempSaveDirectory.resolve("friday-night-game.properties")),
                    "The slugified file should exist in the directory.");

            // Assert Snapshot Integrity
            assertNotNull(loadedSnapshot);
            assertEquals("Friday Night Game", loadedSnapshot.description());
            assertEquals(SaveMode.GAME, loadedSnapshot.mode());
            assertEquals(0, loadedSnapshot.dealerIndex());
            assertEquals(2, loadedSnapshot.players().size());
            assertEquals(1, loadedSnapshot.rounds().size());

            // Assert Player State
            PlayerSnapshot p1 = loadedSnapshot.players().get(0);
            assertEquals("Tommy", p1.name());
            assertEquals(StrategySnapshotType.HUMAN, p1.strategyType());
            assertEquals(10, p1.score());
            assertDoesNotThrow(() -> UUID.fromString(p1.id()), "Restored ID must be a valid UUID");

            // Assert Round State
            RoundSnapshot r1 = loadedSnapshot.rounds().get(0);
            assertEquals(BidType.PASS, r1.bidType());
            assertEquals(Suit.HEARTS, r1.trumpSuit());
        }

        @Test
        @DisplayName("List descriptions retrieves all valid save descriptions")
        void testListDescriptions() {
            // Arrange
            saveRepository.save(testSnapshot);

            List<PlayerSnapshot> players2 = List.of(
                    new PlayerSnapshot(UUID.randomUUID().toString(), "Stan", StrategySnapshotType.HUMAN, 67),
                    new PlayerSnapshot(UUID.randomUUID().toString(), "Seppe", StrategySnapshotType.LOW_BOT, -67)
            );
            GameSnapshot secondSnapshot = new GameSnapshot("Another Game", SaveMode.COUNT, 0, players2, List.of());
            saveRepository.save(secondSnapshot);

            // Act
            List<String> descriptions = saveRepository.listDescriptions();

            // Assert
            assertEquals(2, descriptions.size(), "Should find exactly two saves.");
            assertTrue(descriptions.contains("Friday Night Game"));
            assertTrue(descriptions.contains("Another Game"));
        }

        @Test
        @DisplayName("Fallback to safe defaults when loading older save files missing data")
        void testBackwardCompatibilityLoad() throws IOException {
            // Arrange: Manually write a file simulating an older version (no IDs, no trumpSuit)
            Path saveFile = tempSaveDirectory.resolve("old-save.properties");
            Properties properties = new Properties();
            properties.setProperty("description", "Old Save");
            properties.setProperty("mode", SaveMode.GAME.name());
            properties.setProperty("dealerIndex", "0");

            properties.setProperty("player.count", "1");
            properties.setProperty("player.0.name", "Alice");
            properties.setProperty("player.0.strategy", StrategySnapshotType.HUMAN.name());
            properties.setProperty("player.0.score", "5");
            // Deliberately NOT setting player.0.id

            properties.setProperty("round.count", "1");
            properties.setProperty("round.0.bidType", BidType.PASS.name());
            // Deliberately NOT setting round.0.trumpSuit

            try (var output = Files.newOutputStream(saveFile)) {
                properties.store(output, "Old version save file");
            }

            // Act
            GameSnapshot loaded = saveRepository.loadByDescription("Old Save");

            // Assert: Fallbacks should be applied
            assertDoesNotThrow(() -> UUID.fromString(loaded.players().get(0).id()),
                    "Missing ID should fallback to a newly generated UUID.");
            assertNull(loaded.rounds().get(0).trumpSuit(),
                    "Missing trumpSuit should fallback to 'NONE' mapping (null).");
        }
    }

    @Nested
    @DisplayName("Edge Cases & Defensive Programming")
    class EdgeCaseTests {

        @Test
        @DisplayName("Save handles edge cases and sanitization defensively")
        void testSaveDefensiveAndEdgeCases() {
            assertThrows(IllegalArgumentException.class, () -> saveRepository.save(null));

            GameSnapshot weirdNameSnapshot = new GameSnapshot("  My Wacky!!! Save ???  ", SaveMode.COUNT, 0, players, List.of());
            saveRepository.save(weirdNameSnapshot);

            assertTrue(Files.exists(tempSaveDirectory.resolve("my-wacky-save.properties")),
                    "The description should be safely slugified for the file system.");
        }

        @Test
        @DisplayName("writeSnapshot handles nulls defensively")
        void testWriteSnapshotDefensive() {
            Path validPath = tempSaveDirectory.resolve("test.properties");

            assertAll("Defensive write constraints",
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> saveRepository.writeSnapshot(null, testSnapshot)),
                    () -> assertThrows(IllegalArgumentException.class,
                            () -> saveRepository.writeSnapshot(validPath, null))
            );
        }

        @Test
        @DisplayName("Load by description throws IllegalArgumentException if not found")
        void testLoadByDescriptionNotFound() {
            saveRepository.save(testSnapshot);

            assertThrows(IllegalArgumentException.class, () -> saveRepository.loadByDescription("Non-existent Save"),
                    "Loading a non-existent description should throw an IllegalArgumentException.");
        }

        @Test
        @DisplayName("Load by description handles null input defensively")
        void testLoadByDescriptionDefensive() {
            assertThrows(IllegalArgumentException.class, () -> saveRepository.loadByDescription(null));
        }

        @Test
        @DisplayName("A description that slugifies to an empty string should default to 'unnamed-save'")
        void testSaveAlreadySlugified() {
            testSnapshot = new GameSnapshot("  ", SaveMode.GAME, 0, players, List.of());
            saveRepository.save(testSnapshot);

            assertTrue(Files.exists(tempSaveDirectory.resolve("unnamed-save.properties")));
        }

        @Test
        @DisplayName("List descriptions falls back to filename for blank stored description")
        void testListDescriptionsFallbackForBlankStoredDescription() throws IOException {
            Path saveFile = tempSaveDirectory.resolve("unnamed-save.properties");
            Properties properties = new Properties();
            properties.setProperty("description", "   ");
            properties.setProperty("mode", SaveMode.COUNT.name());
            properties.setProperty("dealerIndex", "0");
            properties.setProperty("player.count", "0");
            properties.setProperty("round.count", "0");

            try (var output = Files.newOutputStream(saveFile)) {
                properties.store(output, "test");
            }

            List<String> descriptions = saveRepository.listDescriptions();
            assertEquals(List.of("unnamed-save"), descriptions);
        }
    }

    @Nested
    @DisplayName("I/O Failure Handling (Mockito Scenarios)")
    class IoFailureTests {

        @Test
        @DisplayName("ensureDirectory throws IllegalStateException when IO fails")
        void testEnsureDirectoryIOException(@TempDir Path tempDir) throws IOException {
            Path conflictingFile = tempDir.resolve("sabotaged_saves");
            Files.createFile(conflictingFile);

            SaveRepository badRepo = new SaveRepository(conflictingFile);

            assertThrows(IllegalStateException.class, badRepo::listDescriptions,
                    "Should throw IllegalStateException wrapping the IOException.");
        }

        @Test
        @DisplayName("readProperties throws IllegalStateException when reading fails")
        void testReadPropertiesIOException(@TempDir Path tempDir) throws IOException {
            SaveRepository repo = new SaveRepository(tempDir);

            Path deceptiveFolder = tempDir.resolve("sneaky.properties");
            Files.createDirectories(deceptiveFolder);

            assertThrows(IllegalStateException.class, repo::listDescriptions,
                    "Should throw IllegalStateException when trying to read a directory as a file.");
        }

        @Test
        @DisplayName("writeSnapshot throws IllegalStateException when IO fails")
        void testWriteSnapshotIOException(@TempDir Path tempDir) throws IOException {
            SaveRepository repo = new SaveRepository(tempDir);

            Path trapFolder = tempDir.resolve("trap.properties");
            Files.createDirectories(trapFolder);

            GameSnapshot trapSnapshot = new GameSnapshot("Trap", SaveMode.COUNT, 0, players, List.of());

            assertThrows(IllegalStateException.class, () -> repo.save(trapSnapshot),
                    "Should throw IllegalStateException when trying to overwrite a directory with a file.");
        }

        @Test
        @DisplayName("listSaveFiles throws IllegalStateException when IO fails")
        void testListSaveFilesIOException(@TempDir Path tempDir) {
            SaveRepository repo = new SaveRepository(tempDir);

            // Hijack the Java Files API safely just for this specific block of code
            try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
                mockedFiles.when(() -> Files.createDirectories(any())).thenCallRealMethod();
                mockedFiles.when(() -> Files.list(any())).thenThrow(new IOException("Simulated OS I/O Crash"));

                IllegalStateException exception = assertThrows(IllegalStateException.class, repo::listDescriptions);

                assertEquals("Failed to list save files", exception.getMessage(),
                        "Should hit the catch block inside listSaveFiles");
            }
        }
    }
}