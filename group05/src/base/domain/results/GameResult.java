package base.domain.results;


public sealed interface GameResult permits AmountOfTrickWonResult, BidSelectionResult, BidTurnResult, BiddingCompleted, EndOfRoundResult, EndOfTrickResult, EndOfTurnResult, ParticipatingPlayersResult, PlayCardResult, PlayerSelectionResult, ProposalRejected, SaveDescriptionResult, ScoreBoardCompleteResult, ScoreBoardResult, StateDoneResult, SuitSelectionRequired, SuitSelectionResult, TrickHistoryResult {

}



