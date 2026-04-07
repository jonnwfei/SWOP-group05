package cli;

import base.domain.bid.BidType;
import base.domain.card.Suit;
import base.domain.commands.*;
import base.domain.results.*;
import cli.elements.Response;
import cli.events.*;
import cli.events.BidEvents.*;

public class Adapter {

    public IOEvent handleResult(GameResult result) {
        return switch (result) {
            case BidTurnResult b         -> new BidTurnIOEvent(b);
            case SuitSelectionRequired s -> new SuitSelectionIOEvent(s);
            case ProposalRejected p      -> new ProposalRejectedIOEvent(p);
            case BiddingCompleted c      -> new BiddingCompletedIOEvent();
        };
    }

    public GameCommand handleResponse(Response response, GameResult result) {
        if (response.rawInput() == null) return new ContinueCommand();

        int choice = Integer.parseInt(response.rawInput());

        return switch (result) {
            case BidTurnResult b ->
                    new BidCommand(BidType.values()[choice - 1]);

            case SuitSelectionRequired s ->
                    new SuitCommand(Suit.values()[choice - 1]);

            case ProposalRejected p ->
                    new BidCommand(choice == 1 ? BidType.PASS : BidType.SOLO_PROPOSAL);

            case BiddingCompleted c -> new ContinueCommand();
        };
    }
}