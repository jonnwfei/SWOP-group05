package cli.adapter;

import base.domain.commands.GameCommand;
import cli.events.IOEvent;

import java.util.List;
import java.util.Objects;

/**
 * @author Stan Kestens, John Cai
 * @since 17/04/2026
 *
 * The AdapterResponse is a record that encapsulates the response from the adapter after processing a GameCommand.
 * Contains the following:
 * <ul>
 *     <li>Original GameCommand</li>
 *     <li>List of IOEvents that should be immediately processed by the UI</li>
 *     <li>Boolean indicating whether the last result should be re-rendered</li>
 * </ul>
 */
public record AdapterResponse(
        GameCommand command,
        List<IOEvent> immediateEvents,
        boolean shouldReRenderLastResult
) {
    /**
     * Defensive constructor that the list of immediate events does not contain null elements.
     * @param command Game Command to possibly wrap into an AdapterResponse
     * @param immediateEvents List of IOEvents that should be immediately processed by the UI
     * @param shouldReRenderLastResult Boolean indicating whether the last result should be re-rendered
     * @throws IllegalArgumentException if the list of immediate events contains null elements
     */
    public AdapterResponse {
        if (immediateEvents.stream().anyMatch(Objects::isNull))
            throw new IllegalArgumentException("Immediate Event list must not contain null elements");
    }

    /**
     * Factory method to create an AdapterResponse from a GameCommand,
     * without any immediate events and false for rerendering the last result.
     * @param command Command to wrap into an AdapterResponse
     * @return AdapterResponse containing command, no immediate events, and false for shouldReRenderLastResult
     */
    public static AdapterResponse toDomain(GameCommand command) {
        return new AdapterResponse(command, List.of(), false);
    }

    /**
     * Factory method to create an AdapterResponse that only contains immediate events,
     * and true to trigger a re-render of the last result.
     * @param events List of IOEvents to include as immediate events in the AdapterResponse
     * @return AdapterResponse containing no command, given immediate events, and true for shouldReRenderLastResult
     */
    public static AdapterResponse uiOnly(IOEvent... events) {
        return new AdapterResponse(null, List.of(events), true);
    }
}