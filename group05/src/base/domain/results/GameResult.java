package base.domain.results;

/**
 * Sealed interface represents the result of a game action or event.
 */
public sealed interface GameResult permits
        AddHumanPlayerResult,
        AmountOfTrickWonResult,
        AddPlayerResult,
        BiddingCompleted,
        BidSelectionResult,
        BidTurnResult,
        CountSaveDescriptionResult,
        DeleteRoundResult,
        EndOfRoundResult,
        EndOfTrickResult,
        EndOfTurnResult,
        GameSaveDescriptionResult,
        ParticipatingPlayersResult,
        PlayCardResult,
        PlayerSelectionResult,
        ProposalRejected,
        ScoreBoardResult,
        SuitSelectionRequired,
        SuitSelectionResult,
        TrickHistoryResult {
}