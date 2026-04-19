package base.domain.results;
/**
 * Sealed interface representing the result of a game action or event.
 */
public sealed interface GameResult permits BidResults, PlayResults, CountResults {}