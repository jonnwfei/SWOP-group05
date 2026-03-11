package base.domain.events.playevents;

import base.domain.events.GameEvent;

/**
 * A marker DTO that signals the user has made their choice on the Scoreboard.
 * The View should catch this, optionally print a transition message,
 * and break its input loop to allow the Controller to transition states.
 */
public record ScoreBoardCompleteEvent() implements GameEvent<String> {
    @Override
    public Class<String> getInputType() {
        return null;
    }

    @Override
    public boolean isValid(String input) {
        // Since it doesn't take input, this will never be evaluated,
        // but returning true for a null Void is technically correct.
        return true;
    }

    @Override
    public boolean needsInput() {
        return false; // This tells the TerminalManager NOT to wait for Scanner input!
    }
}