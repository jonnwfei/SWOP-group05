package base.storage;

import base.domain.bid.BidType;
import base.storage.snapshots.GameSnapshot;
import base.storage.snapshots.PlayerSnapshot;
import base.storage.snapshots.RoundSnapshot;
import base.storage.snapshots.SaveMode;
import base.storage.snapshots.StrategySnapshotType;

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
 * @author John Cai
 * @since 01/04/2026
 */
public class SaveRepository {
    private static final String SAVE_EXTENSION = ".properties";
    private final Path savesDirectory;

    /**
     * Initializes the repository with a default save directory named "saves" in the
     * current working directory.
     */
    public SaveRepository() {
        this(Paths.get("saves"));
    }

    /**
     * Initializes the repository with a custom save directory.
     *
     * @param saveDirectory where saves are stored
     * @throws IllegalArgumentException if saveDirectory is null
     */
    public SaveRepository(Path saveDirectory) {
        if (saveDirectory == null)
            throw new IllegalArgumentException("saveDirectory cannot be null");
        this.savesDirectory = saveDirectory;
    }

    /**
     * Saves the given snapshot to the saveDirectory,
     *
     * @param snapshot to save
     * @throws IllegalArgumentException if snapshot is null
     * @throws IllegalStateException    if the saveDirectory cannot be created
     */
    public void save(GameSnapshot snapshot) {
        if (snapshot == null)
            throw new IllegalArgumentException("Cannot save a null snapshot");

        ensureDirectory();

        Path target = resolveSavePath(snapshot.description());
        writeSnapshot(target, snapshot);
    }

    /**
     * Writes the given snapshot to the specified target path.
     *
     * @param target   target Path to writeSnapshot to
     * @param snapshot snapshot to write
     * @throws IllegalArgumentException if target is null or snapshot is null
     * @throws IllegalStateException    if an IOException occurs during writing
     */
    public void writeSnapshot(Path target, GameSnapshot snapshot) {
        if (target == null)
            throw new IllegalArgumentException("Cannot write to null");
        if (snapshot == null)
            throw new IllegalArgumentException("Cannot write a null snapshot");

        Properties properties = toProperties(snapshot);
        try (OutputStream outputStream = Files.newOutputStream(target)) {
            properties.store(outputStream, "Whist save file");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write save file", e);
        }
    }

    /**
     * Retrieves the list of descriptions of saveFiles
     * If a snapshot doesn't have a description, the file name (without extension)
     * is used instead.
     *
     * @return list of descriptions of all saved snapshots.
     * @throws IllegalStateException if reading directory or files fails
     */
    public List<String> listDescriptions() {
        ensureDirectory();
        List<String> descriptions = new ArrayList<>();
        for (Path saveFile : listSaveFiles()) {
            Properties properties = readProperties(saveFile);
            descriptions.add(resolveDisplayDescription(properties, saveFile));
        }
        return descriptions;
    }

    /**
     * Retrieves the gameSnapshot corresponding to its description
     *
     * @param description of a gameSnapshot
     * @return GameSnapshot
     * @throws IllegalArgumentException if given description is null
     * @throws IllegalArgumentException if no save file is found with the given
     *                                  description
     * @throws IllegalStateException    if a matching saveFile is corrupted or
     *                                  unreadable
     */
    public GameSnapshot loadByDescription(String description) {
        if (description == null)
            throw new IllegalArgumentException("Cannot load from a null description");

        ensureDirectory();
        for (Path saveFile : listSaveFiles()) {
            Properties properties = readProperties(saveFile);
            String storedDescription = resolveDisplayDescription(properties, saveFile);
            if (storedDescription.equals(description)) {
                return fromProperties(properties);
            }
        }
        throw new IllegalArgumentException("No save file found with description: " + description);
    }

    /**
     * Ensures that the saveDirectory is instantiated.
     *
     * @throws IllegalStateException if the save directory cannot be created or
     *                               accessed.
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
     *
     * @return list of Path's (saveFiles)
     * @throws IllegalStateException if IOException occurs during listing of the
     *                               saveDirectory
     */
    private List<Path> listSaveFiles() {
        try (Stream<Path> files = Files.list(savesDirectory)) {
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
     * @throws IllegalStateException if IOException occurs during reading of the
     *                               saveFile
     */
    private Properties readProperties(Path saveFile) {
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(saveFile)) {
            properties.load(inputStream);
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read save file: " + saveFile.getFileName(), e);
        }
    }

    /**
     * Resolves the save path for a given description by slugifying it and appending
     * the .properties extension.
     *
     * @param description to resolve the path from
     * @return Path of this game's savesDirectory + slugified description +
     *         .properties extension
     */
    private Path resolveSavePath(String description) {
        String normalized = description.trim();
        return savesDirectory.resolve(slugify(normalized) + SAVE_EXTENSION);
    }

    /**
     * Retrieves the properties of the given snapshot.
     *
     * @param snapshot to retrieve properties from
     * @return properties of a Snapshot
     */
    private Properties toProperties(GameSnapshot snapshot) {
        Properties properties = new Properties();
        properties.setProperty("description", normalizeDescription(snapshot.description()));
        properties.setProperty("mode", snapshot.mode().name());
        properties.setProperty("dealerIndex", String.valueOf(snapshot.dealerIndex()));
        properties.setProperty("player.count", String.valueOf(snapshot.players().size()));
        properties.setProperty("round.count", String.valueOf(snapshot.rounds().size()));

        for (int i = 0; i < snapshot.players().size(); i++) {
            PlayerSnapshot player = snapshot.players().get(i);
            properties.setProperty("player." + i + ".id", player.id());
            properties.setProperty("player." + i + ".name", player.name());
            properties.setProperty("player." + i + ".strategy", player.strategyType().name());
            properties.setProperty("player." + i + ".score", String.valueOf(player.score()));
        }

        for (int i = 0; i < snapshot.rounds().size(); i++) {
            RoundSnapshot round = snapshot.rounds().get(i);
            String prefix = "round." + i + ".";
            properties.setProperty(prefix + "bidType", round.bidType().name());
            properties.setProperty(prefix + "bidderIndex", String.valueOf(round.bidderIndex()));
            properties.setProperty(prefix + "tricksWon", String.valueOf(round.tricksWon()));
            properties.setProperty(prefix + "multiplier", String.valueOf(round.multiplier()));

            properties.setProperty(prefix + "participants.count", String.valueOf(round.participantIndices().size()));
            for (int j = 0; j < round.participantIndices().size(); j++) {
                properties.setProperty(prefix + "participants." + j, String.valueOf(round.participantIndices().get(j)));
            }

            properties.setProperty(prefix + "miserieWinners.count",
                    String.valueOf(round.miserieWinnerIndices().size()));
            for (int j = 0; j < round.miserieWinnerIndices().size(); j++) {
                properties.setProperty(prefix + "miserieWinners." + j,
                        String.valueOf(round.miserieWinnerIndices().get(j)));
            }

            for (int j = 0; j < round.scoreDeltas().size(); j++) {
                properties.setProperty(prefix + "scoreDelta." + j, String.valueOf(round.scoreDeltas().get(j)));
            }
        }
        return properties;
    }

    /**
     * Constructs a GameSnapshot from the given properties.
     *
     * @param properties to create a gameSnapshot from
     * @return GameSnapshot
     * @throws IllegalStateException if the properties file is missing data or
     *                               malformed
     */
    private GameSnapshot fromProperties(Properties properties) {
        try {
            String description = normalizeDescription(properties.getProperty("description"));
            if (description.isEmpty()) {
                description = "Unnamed Save";
            }
            SaveMode mode = SaveMode.valueOf(properties.getProperty("mode"));
            int dealerIndex = Integer.parseInt(properties.getProperty("dealerIndex"));

            int playerCount = Integer.parseInt(properties.getProperty("player.count", "0"));
            List<PlayerSnapshot> players = new ArrayList<>();
            for (int i = 0; i < playerCount; i++) {
                String id = properties.getProperty("player." + i + ".id", java.util.UUID.randomUUID().toString());                String name = properties.getProperty("player." + i + ".name");
                StrategySnapshotType strategy = StrategySnapshotType
                        .valueOf(properties.getProperty("player." + i + ".strategy"));
                int score = Integer.parseInt(properties.getProperty("player." + i + ".score", "0"));
                players.add(new PlayerSnapshot(id, name, strategy, score));
            }

            int roundCount = Integer.parseInt(properties.getProperty("round.count", "0"));
            List<RoundSnapshot> rounds = new ArrayList<>();
            for (int i = 0; i < roundCount; i++) {
                String prefix = "round." + i + ".";
                BidType bidType = BidType.valueOf(properties.getProperty(prefix + "bidType", BidType.PASS.name()));
                int bidderIndex = Integer.parseInt(properties.getProperty(prefix + "bidderIndex", "0"));
                int tricksWon = Integer.parseInt(properties.getProperty(prefix + "tricksWon", "-1"));
                int multiplier = Integer.parseInt(properties.getProperty(prefix + "multiplier", "1"));

                int participantsCount = Integer.parseInt(properties.getProperty(prefix + "participants.count", "0"));
                List<Integer> participantIndices = new ArrayList<>();
                for (int j = 0; j < participantsCount; j++) {
                    participantIndices.add(Integer.parseInt(properties.getProperty(prefix + "participants." + j, "0")));
                }

                int winnersCount = Integer.parseInt(properties.getProperty(prefix + "miserieWinners.count", "0"));
                List<Integer> miserieWinnerIndices = new ArrayList<>();
                for (int j = 0; j < winnersCount; j++) {
                    miserieWinnerIndices
                            .add(Integer.parseInt(properties.getProperty(prefix + "miserieWinners." + j, "0")));
                }

                List<Integer> scoreDeltas = new ArrayList<>();
                for (int j = 0; j < 4; j++) {
                    scoreDeltas.add(Integer.parseInt(properties.getProperty(prefix + "scoreDelta." + j, "0")));
                }

                rounds.add(new RoundSnapshot(
                        bidType,
                        bidderIndex,
                        participantIndices,
                        tricksWon,
                        miserieWinnerIndices,
                        multiplier,
                        scoreDeltas));
            }
            return new GameSnapshot(description, mode, dealerIndex, players, rounds);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Corrupted save file data detected", e);
        }
    }

    /**
     * Resolves the description shown to users and used for description-based
     * lookup.
     * Missing or blank descriptions fall back to the filename stem.
     */
    private String resolveDisplayDescription(Properties properties, Path saveFile) {
        String stored = properties.getProperty("description");
        String normalized = normalizeDescription(stored);
        return normalized.isEmpty() ? fileNameWithoutExtension(saveFile) : normalized;
    }

    /**
     * Trims user-facing text fields and converts null to empty text.
     */
    private String normalizeDescription(String description) {
        return description == null ? "" : description.trim();
    }

    /**
     * Retrieves a pure filename
     *
     * @param path of a saveFile
     * @return a file name without the extension, or the full file name if no
     *         extension is found
     */
    private String fileNameWithoutExtension(Path path) {
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return fileName.substring(0, dotIndex);
    }

    /**
     * Converts a string into a slug format suitable for file naming.
     * <br>
     * Slugify: e.g. "My Save File! ... = 1" -> "my-save-file-1"
     *
     * @param value String to slugify
     * @return Slugified string
     */
    private String slugify(String value) {
        String slug = value.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return slug.isBlank() ? "unnamed-save" : slug;
    }
}
