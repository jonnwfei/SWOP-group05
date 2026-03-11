package cli;

import base.domain.actions.ContinueAction;
import base.domain.actions.NumberAction;
import base.domain.actions.NumberListAction;
import base.domain.actions.TextAction;
import base.domain.events.GameEvent;
import base.domain.events.errorEvents.NumberErrorEvent;
import base.domain.events.errorEvents.NumberListErrorEvent;
import cli.elements.Response;

import java.util.ArrayList;
import java.util.Scanner;

/**
 * Orchestrates the I/O flow between the user terminal and the domain logic.
 * Manages the render-input-validate loop and handles recursive error recovery.
 *
 * @author Stan Kestens
 * @since 08/03/2026
 */
public class TerminalManager {

    private final Scanner scanner;
    private final TerminalRenderer renderer;
    private final TerminalParser parser;

    /**
     * Initializes the manager with a standard system input scanner.
     */
    public TerminalManager() {
        this.scanner = new Scanner(System.in);
        this.renderer = new TerminalRenderer();
        this.parser = new TerminalParser();
    }

    /**
     * Processes a GameEvent by rendering it and possibly dealing with the input
     * recursivly shows an error message until the right data is entered
     * * @param event The event to process.
     * @return A Response containing the validated user action.
     */
    public Response handle(GameEvent<?> event) {
        try {
            renderer.render(event);
            if (event.needsInput()) {
                return getResponse(event);
            }
            return new Response(false, null);
        } catch (IllegalStateException e) {
            // Handle Integer errors
            if (event.getInputType() == Integer.class) {
                GameEvent<Integer> intEvent = (GameEvent<Integer>) event;
                NumberErrorEvent errorEvent = new NumberErrorEvent(e.getMessage(), intEvent::isValid);
                return handle(errorEvent);
            }
            // Handle String errors (Retry original event)
            if(event.getInputType() == String.class){
                return handle(event);
            }
            // Handle List errors
            @SuppressWarnings("unchecked")
            GameEvent<ArrayList<Integer>> listEvent = (GameEvent<ArrayList<Integer>>) event;
            return handle(new NumberListErrorEvent(listEvent::isValid));
        }
    }

    /**
     * Reads raw input and delegating parsing based on the event's required type.
     * @param event The event requesting input.
     * @return A Response object wrapping the parsed GameAction.
     * @throws IllegalStateException if the input type is unsupported or validation fails.
     */
    private Response getResponse(GameEvent<?> event) {
        String rawInput = scanner.nextLine().trim();
        try {
            if (event.getInputType() == Void.class) {
                return new Response(true, new ContinueAction());
            }

            if (event.getInputType() == String.class) {
                String parsed = parser.parseString(rawInput);
                validateOrThrow(event, parsed);
                return new Response(true, new TextAction(parsed));
            }

            else if (event.getInputType() == Integer.class) {
                Integer parsed = parser.parseNumberInput(rawInput);
                validateOrThrow(event, parsed);
                return new Response(true, new NumberAction(parsed));
            }

            else if (event.getInputType().equals(ArrayList.class) ||
                    event.getInputType().getName().contains("ArrayList")) {
                ArrayList<Integer> parsed = parser.parseNumbersInput(rawInput);
                validateOrThrow(event, parsed);
                return new Response(true, new NumberListAction(parsed));
            }

            throw new IllegalStateException("Unsupported input type: " + event.getInputType());

        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid input: " + e.getMessage());
        }
    }

    /**
     * Validates parsed input against the event's domain rules.
     * @param event The event containing validation logic.
     * @param parsed The parsed data object to check.
     * @throws IllegalArgumentException if validation fails.
     */
    private <T> void validateOrThrow(GameEvent<T> event, Object parsed) {
        @SuppressWarnings("unchecked")
        T typedInput = (T) parsed;
        if (!event.isValid(typedInput)) {
            throw new IllegalArgumentException("Input does not meet the requirements");
        }
    }
}