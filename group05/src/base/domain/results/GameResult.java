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
        DeleteRoundResult,
        EndOfRoundResult,
        EndOfTrickResult,
        EndOfTurnResult,
        ParticipatingPlayersResult,
        PlayCardResult,
        PlayerSelectionResult,
        ProposalRejected,
        SaveDescriptionResult,
        ScoreBoardResult,
        StateDoneResult,
        SuitSelectionRequired,
        SuitSelectionResult,
        TrickHistoryResult {

}