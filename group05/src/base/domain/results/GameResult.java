package base.domain.results;

import base.domain.states.State;

public sealed interface GameResult permits BidTurnResult, BiddingCompleted, ProposalRejected, SuitSelectionRequired {

}