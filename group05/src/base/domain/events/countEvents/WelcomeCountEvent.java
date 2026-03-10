package base.domain.events.countEvents;

import base.domain.events.GameEvent;

/**
 * A Data Transfer Object (DTO) that serves as the entry point for the manual
 * Count/Scoring phase.
 * * * This "marker" event signals the View to display a welcome message and
 * prepare the user for a multi-step sequence of inputs (Bid, Trump, Players,
 * and Tricks) to manually calculate scores for a finished round.
 */
public record WelcomeCountEvent() implements GameEvent {}