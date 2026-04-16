package base.domain.results;


public sealed interface GameResult permits AddHumanPlayerResult, AddPlayerResult, BidSelectionResult, BidTurnResult, BiddingCompleted, DeleteRoundResult, EndOfRoundResult, EndOfTrickResult, EndOfTurnResult, ParticipatingPlayersResult, PlayCardResult, PlayerSelectionResult, ProposalRejected, SaveDescriptionResult, ScoreBoardCompleteResult, ScoreBoardResult, StateDoneResult, SuitSelectionRequired, SuitSelectionResult, TrickHistoryResult, TrickInputResult {

}



