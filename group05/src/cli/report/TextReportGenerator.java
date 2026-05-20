package cli.report;

import java.util.List;

public class TextReportGenerator implements ReportGenerator {

    @Override
    public String generate(ReportData data) {
        StringBuilder sb = new StringBuilder();

        sb.append("========================================\n");
        sb.append("           WHIST GAME REPORT            \n");
        sb.append("========================================\n\n");

        // --- Final rankings ---
        sb.append("FINAL STANDINGS\n");
        sb.append("----------------------------------------\n");
        for (ReportData.RankedPlayer p : data.rankings()) {
            sb.append(String.format("  #%d  %-20s %d pts%n",
                    p.rank(), p.name(), p.finalScore()));
        }
        sb.append("\n");

        // --- Score table ---
        if (data.rounds().isEmpty()) {
            sb.append("No rounds played.\n");
            return sb.toString();
        }

        List<String> playerNames = data.rounds().getFirst().deltas()
                .stream().map(ReportData.PlayerDelta::playerName).toList();

        int nameCol  = 6;
        int trumpCol = 9;
        int bidCol   = 14;
        int scoreCol = Math.max(14,
                playerNames.stream().mapToInt(String::length).max().orElse(14) + 4);

        // header
        sb.append("SCORE TABLE\n");
        sb.append("----------------------------------------\n");
        sb.append(pad("Round", nameCol))
                .append(pad("Trump",  trumpCol))
                .append(pad("Bid",    bidCol));
        playerNames.forEach(n -> sb.append(pad(n, scoreCol)));
        sb.append("\n");

        // rows
        for (ReportData.RoundEntry round : data.rounds()) {
            sb.append(pad("R" + round.roundNumber(), nameCol))
                    .append(pad(round.trumpSuit(),          trumpCol))
                    .append(pad(round.bidType(),            bidCol));
            round.deltas().forEach(d ->
                    sb.append(pad(formatDelta(d.delta()) + " / " + d.runningTotal(), scoreCol)));
            sb.append("\n");
        }

        sb.append("----------------------------------------\n");
        return sb.toString();
    }

    private String pad(String text, int width) {
        if (text == null) text = "";
        if (text.length() >= width) return text.substring(0, width);
        return text + " ".repeat(width - text.length());
    }

    private String formatDelta(int d) { return d > 0 ? "+" + d : String.valueOf(d); }
}