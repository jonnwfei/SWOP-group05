package base.domain.commands;

import base.domain.round.Round;

public record RoundCommand(Round round) implements GameCommand{
    public RoundCommand {
        if (round == null) throw new IllegalArgumentException("round cannot be null");
    }
}
