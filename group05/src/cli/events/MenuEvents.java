package cli.events;

import java.util.List;

public non-sealed interface MenuEvents extends IOEvent {

    record AmountOfBotsIOEvent() implements MenuEvents {
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
}
