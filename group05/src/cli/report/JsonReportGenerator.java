package cli.report;

public class JsonReportGenerator implements ReportGenerator {

    @Override
    public String generate(ReportData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        // --- rankings ---
        sb.append("  \"rankings\": [\n");
        for (int i = 0; i < data.rankings().size(); i++) {
            ReportData.RankedPlayer p = data.rankings().get(i);
            sb.append("    { ")
                    .append("\"rank\": ").append(p.rank()).append(", ")
                    .append("\"name\": \"").append(escape(p.name())).append("\", ")
                    .append("\"finalScore\": ").append(p.finalScore())
                    .append(" }");
            if (i < data.rankings().size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");

        // --- rounds ---
        sb.append("  \"rounds\": [\n");
        for (int i = 0; i < data.rounds().size(); i++) {
            ReportData.RoundEntry round = data.rounds().get(i);
            sb.append("    {\n");
            sb.append("      \"round\": ").append(round.roundNumber()).append(",\n");
            sb.append("      \"trump\": \"").append(escape(round.trumpSuit())).append("\",\n");
            sb.append("      \"bid\": \"").append(escape(round.bidType())).append("\",\n");
            sb.append("      \"scores\": [\n");
            for (int j = 0; j < round.deltas().size(); j++) {
                ReportData.PlayerDelta d = round.deltas().get(j);
                sb.append("        { ")
                        .append("\"player\": \"").append(escape(d.playerName())).append("\", ")
                        .append("\"delta\": ").append(d.delta()).append(", ")
                        .append("\"total\": ").append(d.runningTotal())
                        .append(" }");
                if (j < round.deltas().size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("      ]\n");
            sb.append("    }");
            if (i < data.rounds().size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");

        return sb.toString();
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}