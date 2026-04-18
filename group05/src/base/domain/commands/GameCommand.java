package base.domain.commands;

import base.domain.bid.BidType;
import base.domain.card.Card;
import base.domain.card.Suit;
import base.domain.player.Player;
import base.domain.player.PlayerId;
import base.domain.round.Round;

import java.util.List;

public sealed interface GameCommand {

    record BidCommand(BidType bid, Suit suit) implements GameCommand {
        public BidCommand {
            if (bid == null)
                throw new IllegalArgumentException("bid cannot be null.");
            // suit is intentionally nullable — not all bids require one
        }

        public BidCommand(BidType bid) {
            this(bid, null);
        }
    }

    record CardCommand(Card card) implements GameCommand {
        public CardCommand {
            if (card == null)
                throw new IllegalArgumentException("card cannot be null.");
        }
    }

    record NumberCommand(int choice) implements GameCommand {
        public NumberCommand {
            if (choice < 0)
                throw new IllegalArgumentException("choice cannot be negative.");
        }
    }

    record PlaceBidCommand(BidType bid) implements GameCommand {
        public PlaceBidCommand {
            if (bid == null)
                throw new IllegalArgumentException("bid cannot be null.");
            // suit is intentionally nullable
        }
    }

    record PlayerListCommand(List<PlayerId> playerIds) implements GameCommand {
        public PlayerListCommand {
            if (playerIds == null) {
                throw new IllegalArgumentException("players cannot be null");
            }
        }
    }

    record RoundCommand(Round round) implements GameCommand {
        public RoundCommand {
            if (round == null)
                throw new IllegalArgumentException("round cannot be null.");
        }
    }

    record StartGameCommand() implements GameCommand {}

    record SuitCommand(Suit suit) implements GameCommand {
        public SuitCommand {
            if (suit == null)
                throw new IllegalArgumentException("suit cannot be null.");
        }
    }

    record TextCommand(String text) implements GameCommand {
        public TextCommand {
            if (text == null || text.isBlank())
                throw new IllegalArgumentException("text cannot be null or blank.");
        }
    }
}