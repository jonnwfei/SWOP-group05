package base.storage.snapshots;

import base.domain.bid.BidType;

import java.util.List;
import java.util.Objects;


/**
 * @author John Cai
 * @since 03/04/2026
 *
 * Snapshot of one player for persistence.
 * Containing:
 * <ul>
 *     <li>Player name</li>
 *     <li>StrategyType, bot or player</li>
 *     <li>Score</li>
 * </ul>
 */
public record RoundSnapshot(
        BidType bidType,
        int bidderIndex,
        List<Integer> participantIndices,
        int tricksWon,
        List<Integer> miserieWinnerIndices,
        int multiplier,
        List<Integer> scoreDeltas
        ) {
    /**
     * Defensive constructor for RoundSnapshot
     * @param bidType   bid type of the round, either normal or miserie
     * @param bidderIndex index of the bidder for this round, used for determining turn order and game flow when loading
     * @param participantIndices list of indices of participants for this round, used for determining turn order and game flow when loading
     * @param tricksWon number of tricks won by the bidder, -1 if miserie
     * @param miserieWinnerIndices list of indices of miserie winners for this round, empty if normal bid
     * @param multiplier multiplier for the round, used for calculating score changes when loading
     * @param scoreDeltas list of score changes for each player at the end of the round, used for calculating scores when loading
     * @throws IllegalArgumentException if bidType is null
     * @throws IllegalArgumentException if bidderIndex is negative or greater than 3
     * @throws IllegalArgumentException if multiplier is less than 1
     * @throws IllegalArgumentException if tricksWon is less than -1 or greater than 13
     * @throws IllegalArgumentException if participantIndices is null
     * @throws IllegalArgumentException if scoreDeltas is null or does not contain exactly 4 entries
     * @throws IllegalArgumentException if any participant index is null, negative, or greater than 3
     * @throws IllegalArgumentException if any miserie winner index is null, negative, or greater than 3
     * @throws IllegalArgumentException if miserie winners are not all participants
     * @throws IllegalArgumentException if any score delta is null
     * @throws IllegalArgumentException if score deltas do not sum to zero
     */
    public RoundSnapshot {
        if (bidType == null) throw new IllegalArgumentException("bidType of RoundSnapshot cannot be null");
        if (bidderIndex < 0 || bidderIndex > 3) throw new IllegalArgumentException("bidderIdx can't be negative or greater than 3");
        if (multiplier < 1) throw new IllegalArgumentException("multiplier must be at least 1");
        if (tricksWon < -1 || tricksWon > 13) throw new IllegalArgumentException("tricksWon must be -1 or between 0 and 13");
        if (participantIndices == null) throw new IllegalArgumentException("Participant indices cannot be null");

        if (miserieWinnerIndices == null) miserieWinnerIndices = List.of();

        if (scoreDeltas == null || scoreDeltas.size() != 4)
            throw new IllegalArgumentException("Score deltas must contain exactly 4 entries");

        if (participantIndices.stream().anyMatch(i -> i == null || i < 0 || i > 3))
            throw new IllegalArgumentException("Participant index out of range");

        if (miserieWinnerIndices.stream().anyMatch(i -> i == null || i < 0 || i > 3))
            throw new IllegalArgumentException("Miserie winner index out of range");

        if (!participantIndices.containsAll(miserieWinnerIndices))
            throw new IllegalArgumentException("Miserie winners must be participants");

        if (scoreDeltas.stream().anyMatch(Objects::isNull))
            throw new IllegalArgumentException("Score deltas cannot contain null");

        int sum = scoreDeltas.stream().mapToInt(Integer::intValue).sum();
        if (sum != 0) throw new IllegalArgumentException("Score deltas must be zero-sum, got " + sum);

        // for immutability
        participantIndices = List.copyOf(participantIndices);
        miserieWinnerIndices = List.copyOf(miserieWinnerIndices);
        scoreDeltas = List.copyOf(scoreDeltas);
    }
}
