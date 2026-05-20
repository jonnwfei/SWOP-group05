package base.domain.snapshots;

import base.domain.bid.BidCategory;
import base.domain.bid.BidType;
import base.domain.card.Suit;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;


/**
 * @author John Cai
 * @since 03/04/2026
 *
 * Snapshot of one round for persistence.
 * Containing:
 * <ul>
 *     <li>Identity of the 4 players in the round</li>
 *     <li>Bid type for the round</li>
 *     <li>Bidder index (relative to playerIds)</li>
 *     <li>Participant indices</li>
 *     <li>Number of tricks won by the bidder</li>
 *     <li>Miserie winner indices, if applicable</li>
 *     <li>Round multiplier</li>
 *     <li>Score deltas for each player</li>
 * </ul>
 */
public record RoundSnapshot(
        List<String> playerIds,
        BidType bidType,
        int bidderIndex,
        List<Integer> participantIndices,
        int tricksWon,
        List<Integer> miserieWinnerIndices,
        int multiplier,
        List<Integer> scoreDeltas,
        Suit trumpSuit
        ) {
    /**
     * Defensive constructor for RoundSnapshot
     * @param playerIds the 4 players participating in this specific round
     * @param bidType   bid type of the round, either normal or miserie
     * @param bidderIndex index of the bidder for this round
     * @param participantIndices list of indices of participants for this round
     * @param tricksWon number of tricks won by the bidder, -1 if miserie
     * @param miserieWinnerIndices list of indices of miserie winners for this round, empty if normal bid
     * @param multiplier multiplier for the round
     * @param scoreDeltas list of score changes for each player at the end of the round
     * @throws IllegalArgumentException if bidType is null or playerIds size != 4
     */
    public RoundSnapshot {
        if (playerIds == null || playerIds.size() != 4)
            throw new IllegalArgumentException("Round must have exactly 4 player IDs");
        if (bidType == null) throw new IllegalArgumentException("bidType of RoundSnapshot cannot be null");
        if (bidderIndex < 0 || bidderIndex > 3) throw new IllegalArgumentException("bidderIdx can't be negative or greater than 3: " + bidderIndex);
        if (multiplier < 1) throw new IllegalArgumentException("multiplier must be at least 1: " + multiplier);
        if (tricksWon < -1 || tricksWon > 13) throw new IllegalArgumentException("tricksWon must be -1 or between 0 and 13: " +  tricksWon);
        if (participantIndices == null) throw new IllegalArgumentException("Participant indices cannot be null");

        if (miserieWinnerIndices == null) miserieWinnerIndices = List.of();

        if (scoreDeltas == null || scoreDeltas.size() != 4)
            throw new IllegalArgumentException("Score deltas must contain exactly 4 entries");

        if (participantIndices.stream().anyMatch(i -> i == null || i < 0 || i > 3))
            throw new IllegalArgumentException("Participant index out of range: " +  participantIndices);

        if (miserieWinnerIndices.stream().anyMatch(i -> i == null || i < 0 || i > 3))
            throw new IllegalArgumentException("Miserie winner index out of range: " + miserieWinnerIndices);

        if (!new HashSet<>(participantIndices).containsAll(miserieWinnerIndices))
            throw new IllegalArgumentException("Miserie winners must be participants");

        if (scoreDeltas.stream().anyMatch(Objects::isNull))
            throw new IllegalArgumentException("Score deltas cannot contain null");

        int sum = scoreDeltas.stream().mapToInt(Integer::intValue).sum();
        if (sum != 0) throw new IllegalArgumentException("Score deltas must be zero-sum, got " + sum);

        if (bidType == BidType.PASS && tricksWon != -1) {
            throw new IllegalArgumentException("PASS round must have tricksWon = -1");
        }
        if (bidType != BidType.PASS && participantIndices.isEmpty()) {
            throw new IllegalArgumentException("Non-PASS round must have at least one participant");
        }

        playerIds = List.copyOf(playerIds);
        participantIndices = List.copyOf(participantIndices);
        miserieWinnerIndices = List.copyOf(miserieWinnerIndices);
        scoreDeltas = List.copyOf(scoreDeltas);
    }
}
