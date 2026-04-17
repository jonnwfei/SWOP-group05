package base.domain.results;

import base.domain.trick.Trick;

public record TrickHistoryResult(Trick trick) implements GameResult{
    public TrickHistoryResult {
        if (trick == null) {
            throw new IllegalArgumentException("trick cannot be null");
        }
    }
}
