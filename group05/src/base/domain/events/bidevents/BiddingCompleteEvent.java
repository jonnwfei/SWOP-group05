package base.domain.events.bidevents;

import base.domain.events.GameEvent;

/**
 * A marker DTO that signals the Bidding phase has successfully concluded.
 * The View should catch this, print a transition message, and break its
 * input loop to allow the Controller to transition to the PlayState.
 */
public record BiddingCompleteEvent() implements GameEvent<String> {
    @Override
    public Class getInputType() {
        return null;
    }

    @Override
    public boolean isValid(String input) {
        return true;
    }

    @Override
    public boolean needsInput() {
        return false;
    }

}
