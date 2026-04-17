package base.domain.results;

public record ProposalRejected(String playerName) implements GameResult {

    public ProposalRejected {
        if (playerName == null || playerName.isBlank()) {
            throw new IllegalArgumentException("playerName cannot be null or blank");
        }
    }
}