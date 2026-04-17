package cli.events;

import base.domain.bid.BidType;
import base.domain.player.Player;

import java.util.List;

public sealed interface CountEvents extends IOEvent {

    record BidSelectionIOEvent(BidType[] bidTypes) implements CountEvents {
        public boolean needsInput() {
            return true;
        }
    }

    record PlayerSelectionIOEvent(List<Player> players, boolean multi) implements CountEvents {
        public boolean needsInput() {
            return true;
        }
    }

    record SaveDescriptionIOEvent() implements CountEvents {
        public boolean needsInput() {
            return true;
        }
    }

    record ScoreBoardIOEvent(List<String> playerNames, List<Integer> scores, boolean canRemovePlayer) implements CountEvents {
        public boolean needsInput() {
            return true;
        }
    }

    record TrickInputIOEvent() implements CountEvents {
        public boolean needsInput() {
            return true;
        }
    }
}
