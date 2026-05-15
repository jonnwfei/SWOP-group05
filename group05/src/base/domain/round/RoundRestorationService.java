package base.domain.round;

import base.domain.bid.Bid;
import base.domain.card.Suit;
import base.domain.player.Player;

import java.util.List;
import java.util.Objects;

import static base.domain.WhistRules.MAX_TRICKS;

/**
 * Domain service responsible for restoring a {@link Round} from a persisted snapshot.
 * <p>
 * It validates the provided data and then delegates the actual state mutation
 * to the round via its package‑private {@code restoreState} method.
 *
 * @author Stan Kestens
 * @since 08/05/2026
 */
public class RoundRestorationService {

    /**
     * Restores the round’s internal state from a snapshot.
     *
     * @param round               the round to restore.
     * @param highestBid          restored winning bid.
     * @param trumpSuit           restored trump suit
     * @param participants        restored bidding team members.
     * @param tricksWon           restored tricks‑won value (count‑mode).
     * @param miserieWinners      restored miserie winners (count‑mode).
     * @param restoredScoreDeltas restored per‑player score deltas.
     * @throws IllegalArgumentException if any argument fails validation.
     */
    public void restore(
            Round round,
            Bid highestBid,
            Suit trumpSuit,
            List<Player> participants,
            int tricksWon,
            List<Player> miserieWinners,
            List<Integer> restoredScoreDeltas) {

        validate(round, highestBid, participants, tricksWon, restoredScoreDeltas, miserieWinners);

        round.restoreState(
                highestBid,
                trumpSuit,
                participants,
                tricksWon,
                miserieWinners,
                restoredScoreDeltas);
    }

    private void validate(
            Round round,
            Bid highestBid,
            List<Player> participants,
            int tricksWon,
            List<Integer> restoredScoreDeltas,
            List<Player> miserieWinners) {

        if (highestBid == null) {
            throw new IllegalArgumentException("Cannot restore round without highest bid.");
        }
        if (participants == null
                || participants.isEmpty()
                || participants.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Participants invalid.");
        }
        if (tricksWon < -1 || tricksWon > MAX_TRICKS) {
            throw new IllegalArgumentException("Invalid tricks won value.");
        }
        if (restoredScoreDeltas == null
                || restoredScoreDeltas.size() != round.getPlayers().size()) {
            throw new IllegalArgumentException("Invalid score delta count.");
        }
        if (miserieWinners != null
                && miserieWinners.stream().anyMatch(p -> !participants.contains(p))) {
            throw new IllegalArgumentException("Miserie winners must be participants.");
        }
    }
}