package cli.events;

import base.domain.player.PlayerId;
import base.domain.round.Round;

import java.util.List;
import java.util.Map;

public sealed interface MenuEvents extends IOEvent {

    record AmountOfBotsIOEvent() implements MenuEvents {
        public boolean needsInput() {
            return true;
        }
    }

    record AmountOfHumansIOEvent(int minHumans, int maxHumans) implements MenuEvents {
        public boolean needsInput() {
            return true;
        }
    }

    record BotStrategyIOEvent(int botIndex) implements MenuEvents {
        public boolean needsInput() {
            return true;
        }
    }

    record PlayerNameIOEvent(int playerIndex) implements MenuEvents {
        public boolean needsInput() {
            return true;
        }
    }

    record PrintNamesIOEvent(List<String> playerNames) implements MenuEvents {
        public boolean needsInput() {
            return false;
        }
    }

    record WelcomeMenuIOEvent() implements MenuEvents {
        public boolean needsInput() {
            return true;
        }
    }

    record AddHumanPlayerIOEvent() implements MenuEvents {
        public boolean needsInput() {
            return true;
        }
    }

    record AddPlayerIOEvent() implements MenuEvents {
        public boolean needsInput() {
            return true;
        }
    }

    record DeleteRoundIOEvent(List<Round> rounds) implements MenuEvents {
        public boolean needsInput() {
            return true;
        }
    }

    record LoadSaveIOEvent(List<String> availableSaves) implements MenuEvents {
        public boolean needsInput() { return true; }
    }
    record ScoreTableIOEvent(Map<PlayerId, String> playerColumns, List<Round> rounds) implements MenuEvents{
        public boolean needsInput(){return false;}
    }
}