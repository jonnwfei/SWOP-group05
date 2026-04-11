package base.storage;

import base.domain.snapshots.GameSnapshot;
import base.domain.snapshots.PlayerSnapshot;
import base.domain.snapshots.SaveMode;
import base.domain.snapshots.StrategySnapshotType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * File-based repository for game snapshots.
 *
 * @author johnc
 * @since 01/04/2026
 */
public class SaveRepository {
    private static final String SAVE_EXTENSION = ".properties";
    private final Path savesDirectory;

    /**
     * Initializes the repository with a default save directory named "saves" in the current working directory.
     */
    public SaveRepository() {
        this(Paths.get("saves"));
    }

    /**
     * Initializes the repository with a custom save directory.
     * @param saveDirectory where saves are stored
     * @throws IllegalArgumentException saveDirectory cannot be null
     */
    public SaveRepository(Path saveDirectory) {
        if (saveDirectory == null) throw new IllegalArgumentException("saveDirectory cannot be null");
        this.savesDirectory = saveDirectory;
    }

    /**
     * Saves the given snapshot to the saveDirectory,
     * @param snapshot to save
     * @throws IllegalArgumentException Cannot save a null snapshot
     */
    public void save(GameSnapshot snapshot) {
        if (snapshot == null) throw new IllegalArgumentException("Cannot save a null snapshot");

        ensureDirectory();

        Path target = resolveSavePath(snapshot.description());
        writeSnapshot(target, snapshot);
    }

    public void writeSnapshot(Path target, GameSnapshot snapshot) {
        if (target == null) throw new IllegalArgumentException("Cannot write to null");
        if (snapshot == null) throw new IllegalArgumentException("Cannot write a null snapshot");
        Properties properties = toProperties(snapshot);
        try (OutputStream outputStream = Files.newOutputStream(target)) {
            properties.store(outputStream, "Whist save file");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write save file", e);
        }
    }

    /**
     * Retrieves the list of descriptions of saveFiles
     * If a snapshot doesn't have a description, the file name (without extension) is used instead.
     * @return list of descriptions of all saved snapshots.
     */
    public List<String> listDescriptions() {
        ensureDirectory();
        List<String> descriptions = new ArrayList<>();
        for (Path saveFile : listSaveFiles()) {
            Properties properties = readProperties(saveFile);
            descriptions.add(properties.getProperty("description", fileNameWithoutExtension(saveFile)));
        }
        return descriptions;
    }

    /**
     * Retrieves the gameSnapshot corresponding to its description
     * @param description of a gameSnapshot
     * @return GameSnapshot
     * @throws IllegalArgumentException if given description is null
     */
    public GameSnapshot loadByDescription(String description) {
        if (description == null) throw new IllegalArgumentException("Cannot load from a null description");

        ensureDirectory();
        for (Path saveFile : listSaveFiles()) {
            Properties properties = readProperties(saveFile);
            String storedDescription = properties.getProperty("description", fileNameWithoutExtension(saveFile));
            if (storedDescription.equals(description)) {
                return fromProperties(properties);
            }
        }
        return null;
    }

    /**
     * Ensures that the saveDirectory is instantiated.
     *
     * @throws IllegalStateException if the save directory cannot be created or accessed.
     */
    private void ensureDirectory() {
        try {
            Files.createDirectories(savesDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create save directory", e);
        }
    }

    /**
     * Retrieves a list of saveFiles
     * @return list of Path's (saveFiles)
     */
    private List<Path> listSaveFiles() {
        try (Stream<Path> files = Files.list(savesDirectory)){
            return files.filter(path -> path.getFileName().toString().endsWith(SAVE_EXTENSION))
                    .sorted(Comparator.comparing(Path::getFileName))
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to list save files", e);
        }
    }

    /**
     * Retrieves the properties of a given saveFile
     *
     * @param saveFile to read from
     * @return Properties of a given saveFile
     */
    private Properties readProperties(Path saveFile) {
        if (saveFile == null) throw new IllegalArgumentException("Cannot read properties from a null-saveFile");

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(saveFile)) {
            properties.load(inputStream);
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read save file: " + saveFile.getFileName(), e);
        }
    }

    /**
     *
     * @param description to resolve the path from
     * @return Path of this game's savesDirectory + slugified description + .properties extension
     * @throws IllegalArgumentException if the description is null or empty after trimming
     */
    private Path resolveSavePath(String description) {
        String normalized = description == null ? "" : description.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Save description cannot be empty");
        }
        return savesDirectory.resolve(slugify(normalized) + SAVE_EXTENSION);
    }

    /**
     * Retrieves the properties of the given snapshot.
     *
     * @param snapshot to retrieve properties from
     * @return properties of a Snapshot
     */
    private Properties toProperties(GameSnapshot snapshot) {
        if (snapshot == null) throw new IllegalArgumentException("snapshot cannot be null");

        Properties properties = new Properties();
        properties.setProperty("description", snapshot.description());
        properties.setProperty("mode", snapshot.mode().name());
        properties.setProperty("dealerIndex", String.valueOf(snapshot.dealerIndex() == null ? -1 : snapshot.dealerIndex()));
        properties.setProperty("player.count", String.valueOf(snapshot.players().size()));

        for (int i = 0; i < snapshot.players().size(); i++) {
            PlayerSnapshot player = snapshot.players().get(i);
            properties.setProperty("player." + i + ".name", player.name());
            properties.setProperty("player." + i + ".strategy", player.strategyType().name());
            properties.setProperty("player." + i + ".score", String.valueOf(player.score()));
        }
        return properties;
    }

    /**
     * Constructs a GameSnapshot from the given properties.
     *
     * @param properties to create a gameSnapshot from
     * @return GameSnapshot
     */
    private GameSnapshot fromProperties(Properties properties) {
        String description = properties.getProperty("description", "Unnamed Save");
        SaveMode mode = SaveMode.valueOf(properties.getProperty("mode"));
        int dealerIndexRaw = Integer.parseInt(properties.getProperty("dealerIndex", "-1"));
        Integer dealerIndex = dealerIndexRaw < 0 ? null : dealerIndexRaw;

        int playerCount = Integer.parseInt(properties.getProperty("player.count", "0"));
        List<PlayerSnapshot> players = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            String name = properties.getProperty("player." + i + ".name");
            StrategySnapshotType strategy = StrategySnapshotType.valueOf(properties.getProperty("player." + i + ".strategy"));
            int score = Integer.parseInt(properties.getProperty("player." + i + ".score", "0"));
            players.add(new PlayerSnapshot(name, strategy, score));
        }

        return new GameSnapshot(description, mode, dealerIndex, players);
    }

    /**
     * Retrieves a pure filename
     *
     * @param path of a saveFile
     * @return a file name without the extension, or the full file name if no extension is found
     */
    private String fileNameWithoutExtension(Path path) {
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex >= 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    /**
     * Converts a string into a slug format suitable for file naming.
     * <br>
     * Slugify, e.g. "My Save File!   ... = 1" -> "my-save-file-1"
     *
     * @param value String to slugify
     * @return Slugified string
     */
    private String slugify(String value) {
        String slug = value.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return slug.isBlank() ? "save" : slug;
    }
}

