package base.domain.results;

/**
 * Sealed interface representing the result of a game action or event.
 */
public sealed interface GameResult permits
        AmountOfTrickWonResult,
        BiddingCompleted,
        BidSelectionResult,
        BidTurnResult,
        EndOfRoundResult,
        EndOfTrickResult,
        EndOfTurnResult,
        ParticipatingPlayersResult,
        PlayCardResult,
        PlayerSelectionResult,
        ProposalRejected,
        ScoreBoardResult,
        SuitSelectionRequired,
        SuitSelectionResult,
        TrickHistoryResult {
}