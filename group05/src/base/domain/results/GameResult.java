package base.domain.results;


public sealed interface GameResult permits BidSelectionResult, BidTurnResult, BiddingCompleted, PlayerSelectionResult, ProposalRejected, SaveDescriptionResult, ScoreBoardResult, SuitSelectionRequired, SuitSelectionResult, TrickInputResult {

}



