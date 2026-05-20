package base.domain.round;

import java.util.List;

/**
 * Represents the finalized state and results of a round.
 * Stores the empirical facts and the resulting score deltas.
 */
public record RoundOutcome(
    RoundOutcomeFacts facts,
    List<Integer> scoreDeltas
) {}
