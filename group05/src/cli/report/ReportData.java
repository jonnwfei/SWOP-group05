package cli.report;

import java.util.List;

public record ReportData(
        List<RankedPlayer>  rankings,
        List<RoundEntry>    rounds
) {
    public record RankedPlayer(int rank, String name, int finalScore) {}

    public record RoundEntry(
            int              roundNumber,
            String           trumpSuit,
            String           bidType,
            List<PlayerDelta> deltas
    ) {}

    public record PlayerDelta(String playerName, int delta, int runningTotal) {}
}