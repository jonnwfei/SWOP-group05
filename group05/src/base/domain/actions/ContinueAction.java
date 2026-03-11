package base.domain.actions;

import base.domain.events.GameEvent;

/**
 * Marker for "Press Enter" or passive screen transitions.
 * @author Tommy
 * @since 10/03/2026
 */
public record ContinueAction() implements GameAction {}
