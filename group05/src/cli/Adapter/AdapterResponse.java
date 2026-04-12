package cli.Adapter;

import base.domain.commands.GameCommand;
import cli.events.IOEvent;

import java.util.List;

public record AdapterResponse(
        GameCommand command,
        List<IOEvent> immediateEvents,
        boolean shouldReRenderLastResult
) {
    public static AdapterResponse toDomain(GameCommand cmd) {
        return new AdapterResponse(cmd, List.of(), false);
    }

    public static AdapterResponse uiOnly(IOEvent... events) {
        return new AdapterResponse(null, List.of(events), true);
    }
}