package base.domain.round;

import base.domain.bid.Bid;
import base.domain.bid.BidCategory;
import base.domain.bid.BidType;
import base.domain.player.PlayerId;

import java.util.List;

import static base.domain.WhistRules.MAX_TRICKS;

/**
 * Domain service responsible for determining whether a {@link Round} is finished.
 * <p>
 * It encapsulates all the rules regarding round termination:
 * <ul>
 *   <li>All players passed (all-pass round).</li>
 *   <li>All 13 tricks have been played.</li>
 *   <li>Miserie early termination (every Miserie bidder has taken at least one trick).</li>
 * </ul>
 * Miserie-participant lookup is delegated to the round's
 * {@link base.domain.bid.BidManager}, since {@link Bid} no longer carries a
 * {@link PlayerId}.
 *
 * @author Stan Kestens
 * @since 08/05/2026
 */
public class RoundCompletionService {

    /**
     * Checks whether the given round is finished according to the game rules.
     *
     * @param round the round to evaluate.
     * @return {@code true} if the round is finished.
     */
    public boolean isFinished(Round round) {
        if (round.isMarkedFinished()) {
            return true;
        }
        return isAllPassFinished(round) || shouldAutoFinish(round);
    }

    /**
     * Determines if the round should automatically finish after a trick is played.
     * This is used by the round itself to trigger automatic scoring.
     *
     * @param round the round to evaluate.
     * @return {@code true} if the round conditions warrant an automatic end.
     */
    public boolean shouldAutoFinish(Round round) {
        if (round.getTricks().size() >= MAX_TRICKS) {
            return true;
        }

        Bid highestBid = round.getHighestBid();
        if (highestBid != null && highestBid.getType().getCategory() == BidCategory.MISERIE) {
            return isMiserieEarlyTermination(round);
        }
        return false;
    }

    private boolean isAllPassFinished(Round round) {
        Bid highestBid = round.getHighestBid();
        return highestBid != null
                && highestBid.getType() == BidType.PASS
                && round.getBids().size() == round.getPlayers().size();
    }

    /**
     * Miserie rounds end early when every Miserie bidder
     * has already failed by taking at least one trick.
     * <p>
     * Participants are resolved via the round's
     * {@link base.domain.bid.BidManager} rather than by inspecting individual
     * {@link Bid} objects, which no longer reference {@link PlayerId}.
     */
    private boolean isMiserieEarlyTermination(Round round) {
        Bid highestBid = round.getHighestBid();
        if (highestBid == null || highestBid.getType().getCategory() != BidCategory.MISERIE) {
            return false;
        }

        List<PlayerId> miserieBidders =
                round.getBidManager().findMiserieParticipants(highestBid.getType());

        if (miserieBidders.isEmpty()) {
            return false;
        }

        for (PlayerId bidderId : miserieBidders) {
            boolean wonATrick = round.getTricks().stream()
                    .anyMatch(trick -> trick.getWinningPlayerId().equals(bidderId));
            if (!wonATrick) {
                return false;   // someone is still safe
            }
        }
        return true;            // all Miserie bidders have failed
    }
}