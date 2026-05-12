package cli.report;

import base.GameController;
import base.domain.player.Player;
import base.domain.round.Round;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ReportService {

    private static final DateTimeFormatter FILE_TS =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public ReportData buildReportData(GameController controller) {
        List<String>  names  = controller.getPlayerNames();
        List<Integer> scores = controller.getPlayerScores();
        List<Round>   rounds = controller.getRounds();

        // --- rankings (sorted high → low) ---
        record Pair(String name, int score) {}
        List<Pair> pairs = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) pairs.add(new Pair(names.get(i), scores.get(i)));
        pairs.sort(Comparator.comparingInt(Pair::score).reversed());

        List<ReportData.RankedPlayer> rankings = new ArrayList<>();
        for (int i = 0; i < pairs.size(); i++) {
            rankings.add(new ReportData.RankedPlayer(i + 1, pairs.get(i).name(), pairs.get(i).score()));
        }

        // --- score table ---
        List<ReportData.RoundEntry> roundEntries = new ArrayList<>();
        int[] runningTotals = new int[names.size()];

        for (int ri = 0; ri < rounds.size(); ri++) {
            Round         round        = rounds.get(ri);
            List<Player>  roundPlayers = round.getPlayers();
            List<Integer> deltas       = round.getScoreDeltas();

            String trump = round.getTrumpSuit() != null ? round.getTrumpSuit().name() : "—";
            String bid   = round.getHighestBid() != null ? round.getHighestBid().getType().name() : "—";

            List<ReportData.PlayerDelta> playerDeltas = new ArrayList<>();
            for (int pi = 0; pi < names.size(); pi++) {
                int delta = (pi < roundPlayers.size() && pi < deltas.size()) ? deltas.get(pi) : 0;
                runningTotals[pi] += delta;
                playerDeltas.add(new ReportData.PlayerDelta(names.get(pi), delta, runningTotals[pi]));
            }

            roundEntries.add(new ReportData.RoundEntry(ri + 1, trump, bid, playerDeltas));
        }

        return new ReportData(rankings, roundEntries);
    }

    public String generate(ReportData data, ReportFormat format) {
        ReportGenerator generator = switch (format) {
            case TEXT     -> new TextReportGenerator();
            case MARKDOWN -> new MarkdownReportGenerator();
            case JSON     -> new JsonReportGenerator();
        };
        return generator.generate(data);
    }

    /** Saves to ./reports/whist_<timestamp>.<ext> and returns the path. */
    public Path save(String content, ReportFormat format) throws IOException {
        Path dir = Path.of("reports");
        Files.createDirectories(dir);
        String filename = "whist_" + LocalDateTime.now().format(FILE_TS)
                + "." + format.extension();
        Path file = dir.resolve(filename);
        Files.writeString(file, content);
        return file;
    }
}