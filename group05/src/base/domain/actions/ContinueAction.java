package base.domain.actions;

import base.domain.events.GameEvent;

/**
 * Marker for "Press Enter" or passive screen transitions.
 */
public record ContinueAction() implements GameAction {}
