package base.domain.scores;


public record ScoringParameters(
        int minTricks,
        int maxTricks,
        int basePoints,
        int overtrickPoints,
        boolean doublesOnAllTricks
) {

    public int calculatePoints(int tricksWon) {
        if (tricksWon < minTricks || tricksWon > maxTricks) {
            return -basePoints;
        }

        int excessTricks = tricksWon - minTricks;
        int totalPoints = basePoints + (excessTricks * overtrickPoints);

        //TODO: change hardcoded 13 to MAX_TRICKS in WhistRules
        if (doublesOnAllTricks && tricksWon == 13) {
            totalPoints *= 2;
        }

        return totalPoints;
    }
}


