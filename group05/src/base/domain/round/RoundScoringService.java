package base.domain.round;

import base.domain.bid.Bid;
import base.domain.bid.BidCategory;
import base.domain.player.Player;

import java.util.List;

/**
 * Pure domain service responsible for all scoring calculations within a {@link Round}.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Calculating trick outcomes for standard and Miserie contracts.</li>
 *   <li>Distributing score deltas in a zero‑sum fashion.</li>
 *   <li>Applying final score updates to each player and to the round’s delta record.</li>
 * </ul>
 * This service contains no lifecycle or orchestration concerns.
 * @author Stan Kestens
 * @since 08/05/2026
 */
public class RoundScoringService {

    /**
     * Calculates and distributes scores for a fully played round.
     * The round’s finished flag is set as a side effect via {@link Round#markFinished()}.
     *
     * @param round the round for which scores must be calculated.
     * @throws IllegalStateException if the round has no highest bid.
     */
    public void calculateScores(Round round) {
        Bid highestBid = round.getHighestBid();
        if (highestBid == null) {
            throw new IllegalStateException("Cannot calculate scores without a highest bid.");
        }

        if (highestBid.getType().getCategory() == BidCategory.MISERIE) {
            calculateMiserieScores(round);
        } else {
            calculateStandardScores(round);
        }

        round.markFinished();
    }

    private void calculateMiserieScores(Round round) {
        List<Player> bidders = round.getBiddingTeamPlayers();
        for (Player p : bidders) {
            int tricks = resolvePlayerTricks(round, p);
            distributeScores(
                    round,
                    round.getHighestBid().calculateBasePoints(tricks),
                    List.of(p));
        }
    }

    private void calculateStandardScores(Round round) {
        int tricksWon = resolveTeamTricks(round);
        distributeScores(
                round,
                round.getHighestBid().calculateBasePoints(tricksWon),
                round.getBiddingTeamPlayers());
    }

    private int resolvePlayerTricks(Round round, Player player) {
        // If real tricks were played, count them
        if (!round.getTricks().isEmpty()) {
            return round.getTricksWonBy(List.of(player));
        }
        // For restored / count‑mode rounds: use recorded miserie winners
        return round.getCountMiserieWinners().contains(player) ? 0 : 1;
    }

    private int resolveTeamTricks(Round round) {
        // For restored / count‑mode rounds the explicit value is stored
        if (round.getCountTricksWon() >= 0) {
            return round.getCountTricksWon();
        }
        // Normal gameplay: count actually won tricks
        return round.getTricksWonBy(round.getBiddingTeamPlayers());
    }

    /**
     * Applies zero‑sum score changes to all players.
     *
     * @param round     the round whose scores are being distributed.
     * @param basePoints the base points calculated from the contract.
     * @param bidders   the players in the bidding team (attacking side).
     * @throws IllegalStateException if the point distribution is inconsistent with team size.
     */
    public void distributeScores(Round round, int basePoints, List<Player> bidders) {
        int multiplier = round.getMultiplier();

        if (bidders.size() == 1 && (basePoints * multiplier) % 3 != 0) {
            throw new IllegalStateException(
                    "Base points must be divisible by 3 for a 1v3 round to maintain zero‑sum.");
        }

        List<Player> players = round.getPlayers();
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            int delta;

            if (bidders.contains(player)) {
                delta = basePoints * multiplier;
            } else {
                if (bidders.size() == 2) {
                    delta = basePoints * multiplier * -1;
                } else {
                    delta = (basePoints * multiplier * -1) / 3;
                }
            }

            player.updateScore(delta);
            round.addScoreDelta(i, delta);
        }
    }
}