package base.domain.results;

public sealed interface GameResult permits BidSelectionResult, BidTurnResult, BiddingCompleted, EndOfRoundResult,
        EndOfTrickResult, EndOfTurnResult, ParticipatingPlayersResult, PlayCardResult, PlayerSelectionResult,
        ProposalRejected, SaveDescriptionResult, ScoreBoardResult, SuitSelectionRequired, SuitSelectionResult,
        TrickHistoryResult, AmountOfTrickWonResult {

}
