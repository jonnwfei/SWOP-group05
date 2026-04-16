package cli.events;

import base.domain.results.BidTurnResult;
import base.domain.results.ProposalRejected;

public non-sealed interface BidEvents extends IOEvent {

    record BiddingCompletedIOEvent() implements BidEvents {
        public boolean needsInput() {
            return false;
        }
    }

    record BidTurnIOEvent(BidTurnResult data) implements BidEvents {
        public boolean needsInput() {
            return true;
        }
    }

    record ProposalRejectedIOEvent(ProposalRejected data) implements BidEvents {
        public boolean needsInput() {
            return true;
        }
    }

    record SuitSelectionIOEvent() implements BidEvents {
        public boolean needsInput() {
            return true;
        }
    }

}
