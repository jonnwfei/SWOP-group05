package base.domain.commands;

import base.domain.bid.BidType;


import base.domain.card.Suit;

public record BidCommand(BidType bid, Suit suit) implements GameCommand {
    public BidCommand(BidType bid) {
        if (bid == null) {
            throw new IllegalArgumentException("bid cannot be null");
        }
        this(bid, null);
    }
}