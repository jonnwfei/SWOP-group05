import java.util.List;

/**
 * Represents an unresolved request for a partner.
 * MUST be resolved into AcceptedBid, SoloProposalBid or PassBid before the Playing Phase.
 */
public record ProposalBid(Player proposer) implements Bid {

    @Override
    public List<Player> getTeam() {
        return List.of(proposer);
    }

    @Override
    public BidRank getRank() {
        return BidRank.PROPOSAL;
    }

    // --- DEFENSIVE PROGRAMMING BELOW ---
    // These methods belong to the Playing/Scoring phase.
    // If they are called, the engine has a bug.

    @Override
    public Suit getChosenTrump(Suit dealtTrump) {
        throw new IllegalStateException("CRITICAL: An unresolved ProposalBid reached the play phase!");
    }

    @Override
    public boolean checkWin(List<Trick> teamTricks) {
        throw new IllegalStateException("CRITICAL: Cannot evaluate a win for an unresolved ProposalBid!");
    }

    @Override
    public int calculateBasePoints(int wonTricks) {
        throw new IllegalStateException("CRITICAL: Cannot score an unresolved ProposalBid!");
    }
}