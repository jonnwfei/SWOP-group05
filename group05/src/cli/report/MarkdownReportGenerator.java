package cli.report;

import java.util.List;

public class MarkdownReportGenerator implements ReportGenerator {

    @Override
    public String generate(ReportData data) {
        StringBuilder sb = new StringBuilder();

        sb.append("# Whist Game Report\n\n");

        // --- Final rankings ---
        sb.append("## Final Standings\n\n");
        sb.append("| Rank | Player | Score |\n");
        sb.append("|------|--------|-------|\n");
        for (ReportData.RankedPlayer p : data.rankings()) {
            sb.append(String.format("| #%d | %s | %d |\n",
                    p.rank(), p.name(), p.finalScore()));
        }
        sb.append("\n");

        // --- Score table ---
        if (data.rounds().isEmpty()) {
            sb.append("_No rounds played._\n");
            return sb.toString();
        }

        List<String> playerNames = data.rounds().getFirst().deltas()
                .stream().map(ReportData.PlayerDelta::playerName).toList();

        sb.append("## Score Table\n\n");

        // header
        sb.append("| Round | Trump | Bid |");
        playerNames.forEach(n -> sb.append(" ").append(n).append(" |"));
        sb.append("\n");

        // separator
        sb.append("|-------|-------|-----|");
        playerNames.forEach(n -> sb.append("---|"));
        sb.append("\n");

        // rows
        for (ReportData.RoundEntry round : data.rounds()) {
            sb.append(String.format("| R%d | %s | %s |",
                    round.roundNumber(), round.trumpSuit(), round.bidType()));
            round.deltas().forEach(d ->
                    sb.append(String.format(" %s / %d |",
                            formatDelta(d.delta()), d.runningTotal())));
            sb.append("\n");
        }

        return sb.toString();
    }

    private String formatDelta(int d) { return d > 0 ? "+" + d : String.valueOf(d); }
}