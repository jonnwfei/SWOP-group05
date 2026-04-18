package cli.events;


import base.domain.results.PlayResults.*;

public sealed interface PlayEvents extends IOEvent {

    record ConfirmationIOEvent(String playerName) implements PlayEvents {
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

    record TrickHistoryIOEvent(TrickHistoryResult data) implements PlayEvents {
        public boolean needsInput() {
            return false;
        }
    }
}
