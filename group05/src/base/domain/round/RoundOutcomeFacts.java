package base.domain.round;

import base.domain.player.PlayerId;

import java.util.List;

/**
 * Represents the minimal set of empirical facts needed to score a round.
 * Decouples the scoring logic from how the results were obtained (In-App vs Manual).
 *
 * @param tricksWon The number of tricks won by the bidding team.
 * @param miserieWinners The list of players who successfully "won" their miserie (didn't take any tricks).
 */
public record RoundOutcomeFacts(
    int tricksWon,
    List<PlayerId> miserieWinners
) {}
