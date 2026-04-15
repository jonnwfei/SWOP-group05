package base.domain.results;

import base.domain.trick.Trick;

public record TrickHistoryResult(Trick trick) implements GameResult{
}
