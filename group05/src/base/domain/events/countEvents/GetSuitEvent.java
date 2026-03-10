package base.domain.events.countEvents;

import base.domain.events.GameEvent;

/**
 * A Data Transfer Object (DTO) used during the Count/Scoring phase to request
 * the trump suit of the round being manually entered.
 * * Unlike the active Bidding phase, this event is part of a manual scoring
 * sequence where the user provides historical data about a completed round.
 */
public record GetSuitEvent() implements GameEvent {

}