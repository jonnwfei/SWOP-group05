package cli.events.PlayEvents;

import base.domain.card.Card;
import cli.events.IOEvent;

public record BotCardIOEvent(Card card) implements IOEvent {
    @Override
    public boolean needsInput() {
        return false;
    }

    @Override
    public boolean getContinue() {
        return true;
    }
}
