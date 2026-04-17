package cli.events;

import base.domain.card.Card;
import base.domain.results.*;
import base.domain.trick.Trick;

public non-sealed interface PlayEvents extends IOEvent {

    record BotCardIOEvent(Card card) implements PlayEvents {
        @Override
        public boolean needsInput() {
            return false;
        }
    }

    record ConfirmationIOEvent(String playerName) implements PlayEvents {
        @Override
        public boolean needsInput() {
            return true;
        }
    }

    record EndOfRoundIOEvent(EndOfRoundResult data) implements PlayEvents {
        public boolean needsInput() {
            return false;
        }
    }

    record EndOfTrickIOEvent(EndOfTrickResult data) implements PlayEvents {
        public boolean needsInput() {
            return false;
        }
    }

    record EndOfTurnIOEvent(EndOfTurnResult data) implements PlayEvents {
        public boolean needsInput() {
            return false;
        }
    }

    record ParticipatingPlayersIOEvent(ParticipatingPlayersResult data) implements PlayEvents {
        public boolean needsInput() {
            return true;
        }
    }

    record PlayCardIOEvent(PlayCardResult data) implements PlayEvents {
        public boolean needsInput() {
            return true;
        }
    }

    record ShowLastTrickIOEvent(Trick lastTrick) implements PlayEvents {
        @Override
        public boolean needsInput() {
            return false;
        }
    }

    record TrickHistoryIOEvent(TrickHistoryResult data) implements PlayEvents {
        public boolean needsInput() {
            return false;
        }
    }
}
