package base.storage;

import base.storage.snapshots.GameSnapshot;
import base.storage.snapshots.PlayerSnapshot;
import base.storage.snapshots.RoundSnapshot;
import base.storage.snapshots.SaveMode;
import base.storage.snapshots.StrategySnapshotType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

class SaveRepositoryTest {

    // JUnit 5 automatically creates and cleans up a real temp directory
    @TempDir
    Path tempSaveDirectory;

    private SaveRepository saveRepository;
    private GameSnapshot testSnapshot;
    private List<PlayerSnapshot> players;
    private List<RoundSnapshot> rounds;

    @BeforeEach
    void setUp() {
        saveRepository = new SaveRepository(tempSaveDirectory);

        players = List.of(
                new PlayerSnapshot("Tommy", StrategySnapshotType.HUMAN, 10),
                new PlayerSnapshot("Seppe", StrategySnapshotType.HIGH_BOT, -10));
        rounds = List.of(
                new RoundSnapshot(base.domain.bid.BidType.PASS, 0, List.of(0), -1, List.of(), 1, List.of(0, 0, 0, 0)));
        testSnapshot = new GameSnapshot("Friday Night Game", SaveMode.GAME, 0, players, rounds);
    }

    @Test
    @DisplayName("Constructor handles illegal arguments defensively")
    void testConstructorDefensive() {
        assertThrows(IllegalArgumentException.class, () -> new SaveRepository(null),
                "Initializing with a null Path should throw an exception.");
    }

    @Test
    @DisplayName("Default constructor initializes without throwing")
    void testDefaultConstructor() {
        assertDoesNotThrow(() -> new SaveRepository(),
                "Default constructor should fall back to 'saves' directory safely.");
    }

    @Test
    @DisplayName("Save and Load Main Success Scenario")
    void testSaveAndLoadSuccess() {
        saveRepository.save(testSnapshot);

        assertTrue(Files.exists(tempSaveDirectory.resolve("friday-night-game.properties")),
                "The slugified file should exist in the directory.");

        GameSnapshot loadedSnapshot = saveRepository.loadByDescription("Friday Night Game");

        assertNotNull(loadedSnapshot);
        assertEquals("Friday Night Game", loadedSnapshot.description());
        assertEquals(SaveMode.GAME, loadedSnapshot.mode());
        assertEquals(0, loadedSnapshot.dealerIndex());
        assertEquals(2, loadedSnapshot.players().size());
        assertEquals(1, loadedSnapshot.rounds().size());

        assertEquals("Tommy", loadedSnapshot.players().get(0).name());
        assertEquals(StrategySnapshotType.HUMAN, loadedSnapshot.players().get(0).strategyType());
        assertEquals(10, loadedSnapshot.players().get(0).score());
    }

    @Test
    @DisplayName("Save handles edge cases and sanitization defensively")
    void testSaveDefensiveAndEdgeCases() {
        assertThrows(IllegalArgumentException.class, () -> saveRepository.save(null));

        GameSnapshot weirdNameSnapshot = new GameSnapshot("  My Wacky!!! Save ???  ", SaveMode.COUNT, 0, players,
                List.of());
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
                        () -> saveRepository.writeSnapshot(validPath, null)));
    }

    @Test
    @DisplayName("List descriptions retrieves all valid save descriptions")
    void testListDescriptions() {
        saveRepository.save(testSnapshot);

        List<PlayerSnapshot> players2 = List.of(
                new PlayerSnapshot("Stan", StrategySnapshotType.HUMAN, 67),
                new PlayerSnapshot("Seppe", StrategySnapshotType.LOW_BOT, -67));

        GameSnapshot secondSnapshot = new GameSnapshot("Another Game", SaveMode.COUNT, 0, players2, List.of());
        saveRepository.save(secondSnapshot);

        List<String> descriptions = saveRepository.listDescriptions();

        assertEquals(2, descriptions.size(), "Should find exactly two saves.");
        assertTrue(descriptions.contains("Friday Night Game"));
        assertTrue(descriptions.contains("Another Game"));
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
    @DisplayName("Load by description handles null input defensively")
    void testSaveAlreadySlugified() {
        testSnapshot = new GameSnapshot("  ", SaveMode.GAME, 0, players, List.of());
        saveRepository.save(testSnapshot);
        assertTrue(Files.exists(tempSaveDirectory.resolve("unnamed-save.properties")),
                "A description that slugifies to an empty string should default to 'unnamed-save'.");
    }

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

        // Hijack the Java Files API just for this specific block of code
        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {

            mockedFiles.when(() -> Files.createDirectories(any())).thenCallRealMethod();

            mockedFiles.when(() -> Files.list(any())).thenThrow(new IOException("Simulated Windows I/O Crash"));

            IllegalStateException exception = assertThrows(IllegalStateException.class, repo::listDescriptions);

            assertEquals("Failed to list save files", exception.getMessage(),
                    "Should hit the catch block inside listSaveFiles");
        }
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