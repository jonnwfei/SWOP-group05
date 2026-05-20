package base.domain.scores;

/**
 * An immutable Value Object defining the mathematical parameters for a Whist Bid.
 */
public record ScoringParameters(
        int minTricks,
        int maxTricks,
        int basePoints,
        int overtrickPoints,
        boolean doublesOnAllTricks
) {
    // Compact constructor for defensive programming
    public ScoringParameters {
        if (minTricks < 0) {
            throw new IllegalArgumentException("Minimum tricks cannot be negative.");
        }
        if (maxTricks < minTricks) {
            throw new IllegalArgumentException("Maximum tricks cannot be less than minimum tricks.");
        }
        if (basePoints < 0) {
            throw new IllegalArgumentException("Base points must be positive (losses calculate the negative internally).");
        }
        if (overtrickPoints < 0) {
            throw new IllegalArgumentException("Overtrick points cannot be negative.");
        }
    }

    public int calculatePoints(int tricksWon) {
        if (tricksWon < minTricks || tricksWon > maxTricks) {
            return -basePoints;
        }

        int excessTricks = tricksWon - minTricks;
        int totalPoints = basePoints + (excessTricks * overtrickPoints);

        // TODO: change hardcoded 13 to MAX_TRICKS in WhistRules
        if (doublesOnAllTricks && tricksWon == 13) {
            totalPoints *= 2;
        }

        return totalPoints;
    }
}