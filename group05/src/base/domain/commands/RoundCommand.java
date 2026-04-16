package base.domain.commands;

import base.domain.round.Round;

public record RoundCommand(Round round) implements GameCommand{
}
