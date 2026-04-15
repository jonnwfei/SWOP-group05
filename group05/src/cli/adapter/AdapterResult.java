package cli.adapter;

import base.domain.commands.GameCommand;
import cli.events.IOEvent;

public sealed interface AdapterResult {
    record NeedsIO(IOEvent event) implements AdapterResult {}
    record Immediate(GameCommand command) implements AdapterResult {}
}