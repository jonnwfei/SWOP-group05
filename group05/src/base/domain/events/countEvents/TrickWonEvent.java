package base.domain.events.countEvents;

import base.domain.events.GameEvent;

/**
 * A Data Transfer Object (DTO) used during the manual Count phase to request
 * the total number of tricks won by the bidding players.
 * * * This event is triggered after the Bid, Trump, and Participating Players
 * have been identified. The Domain uses this input to calculate the final
 * score based on the specific bid requirements (e.g., 9 tricks for Abondance).
 */
public record TrickWonEvent() implements GameEvent {}
