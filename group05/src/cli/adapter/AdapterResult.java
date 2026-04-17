package cli.adapter;

import base.domain.commands.GameCommand;
import cli.events.IOEvent;

import java.util.List;

public sealed interface AdapterResult {
    record NeedsIO(List<IOEvent> preamble, IOEvent event) implements AdapterResult {}
    record Immediate(GameCommand command) implements AdapterResult {}
}