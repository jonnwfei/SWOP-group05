package base.domain.results;

public sealed interface GameResult permits AddHumanPlayerResult, AddPlayerResult, BidSelectionResult, BidTurnResult, BiddingCompleted, DeleteRoundResult, EndOfRoundResult,
        EndOfTrickResult, EndOfTurnResult, ParticipatingPlayersResult, PlayCardResult, PlayerSelectionResult,
        ProposalRejected, SaveDescriptionResult, ScoreBoardResult, StateDoneResult, SuitSelectionRequired, SuitSelectionResult,
        TrickHistoryResult, AmountOfTrickWonResult {

}